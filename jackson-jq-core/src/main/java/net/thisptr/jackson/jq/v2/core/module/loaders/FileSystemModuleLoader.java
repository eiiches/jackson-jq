package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Reads modules and data off the filesystem, searching a fixed list of absolute search paths.
 * <p>
 * Each module it returns remembers the search path it was found under, which is what an import
 * relative to that module resolves against -- and what such an import may not escape.
 * <p>
 * <b>What the boundary means.</b> No path this loader opens is outside the search path it was
 * resolved under: an import path may not be absolute, may not contain a {@code ..} component, may
 * not name the search path itself, and every candidate file is checked against the search path
 * before the filesystem is touched. A {@code {search: ...}} override is bounded the same way,
 * against the search path the importing module was found under.
 * <p>
 * That check is on the paths, not on what the filesystem does with them: a symlink inside the
 * search path is followed wherever it points, exactly as jq does. A symlink in the tree is the
 * operator's doing, and anyone able to put one there could equally put a {@code .jq} file there,
 * which is executable code rather than a read. A search path whose contents are not trusted is
 * therefore not a sandbox, and was never one.
 */
public class FileSystemModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final List<Path> searchPaths;
	private final JsonProvider<JsonNode> jsonProvider;

	public FileSystemModuleLoader(JsonProvider<JsonNode> jsonProvider, Path... searchPaths) {
		List<Path> absoluteSearchPaths = new ArrayList<>();
		for (Path searchPath : searchPaths) {
			if (!searchPath.isAbsolute())
				throw new RuntimeException("Search path must be absolute");
			// Containment is decided by startsWith against these, which only means anything if they
			// are normalized: /a/b/../c and /a/c must not be two different boundaries.
			absoluteSearchPaths.add(searchPath.normalize());
		}
		this.searchPaths = absoluteSearchPaths;
		this.jsonProvider = jsonProvider;
	}

	/**
	 * A module file this loader read. It knows both where it is and which search path it was found
	 * under, which is everything an import written inside it needs to resolve.
	 */
	private static final class FileSystemJqModule<JsonNode> implements JqModule<JsonNode> {
		private final Path searchPath;
		private final Path modulePath;
		private final String source;
		private final JsonProvider<JsonNode> jsonProvider;

		FileSystemJqModule(Path searchPath, Path modulePath, String source, JsonProvider<JsonNode> jsonProvider) {
			this.searchPath = searchPath;
			this.modulePath = modulePath;
			this.source = source;
			this.jsonProvider = jsonProvider;
		}

		@Override
		public String getSourceCode() {
			return source;
		}

		/**
		 * Resolves {@code searchPath} against this module's own directory, then {@code importPath}
		 * against that -- {@code {search: "./"}} meaning "next to this file".
		 * <p>
		 * jq's C code has no equivalent bound: {@code build_lib_search_chain} concatenates and
		 * leaves it there. Refusing to leave the search path the importing module was found under is
		 * this loader's own hardening.
		 */
		private Path resolveOverride(String importPath, String override) throws JsonQueryException {
			Path overridePath = modulePath.getFileSystem().getPath(override);
			Path resolvedOverride = Objects.requireNonNull(modulePath.getParent()).resolve(overridePath).normalize();
			if (!resolvedOverride.startsWith(searchPath))
				throw new JsonQueryException("search path overrides from import metadata must stay within the original search path of the caller module");
			return resolveModulePath(resolvedOverride, importPath);
		}

		@Override
		public JqModule<JsonNode> relativeImport(String importPath, String searchPathOverride) throws JsonQueryException {
			Path resolvedPath = resolveOverride(importPath, searchPathOverride);
			Path filePath = findFile(searchPath, resolvedPath, "jq");
			if (filePath == null)
				throw new ModuleNotFoundException(importPath);
			// Still the caller's search path: a module reached through an override belongs to the
			// same tree, so its own relative imports are bounded the same way.
			return new FileSystemJqModule<>(searchPath, filePath, read(filePath, "module", importPath), jsonProvider);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPathOverride) throws JsonQueryException {
			Path resolvedPath = resolveOverride(importPath, searchPathOverride);
			Path filePath = findFile(searchPath, resolvedPath, "json");
			if (filePath == null)
				throw new ModuleNotFoundException(importPath);
			return parseData(jsonProvider, filePath, importPath);
		}

		/**
		 * Two instances of the same module file are the same module, however they were reached: the
		 * compiler compiles each distinct module once and detects cycles by re-entry.
		 */
		@Override
		public boolean equals(@Nullable Object o) {
			if (!(o instanceof FileSystemJqModule))
				return false;
			return modulePath.equals(((FileSystemJqModule<?>) o).modulePath);
		}

		@Override
		public int hashCode() {
			return modulePath.hashCode();
		}

		@Override
		public String toString() {
			return modulePath.toString();
		}
	}

	/**
	 * Resolves an import path against a directory, refusing anything that could name a file outside
	 * it. Like jq's {@code validate_relpath}, a {@code ..} component is refused outright rather than
	 * normalized away, so no import path can be written that even points at the parent.
	 */
	private static Path resolveModulePath(Path base, String path) throws JsonQueryException {
		Path modulePath = base.getFileSystem().getPath(path);
		if (modulePath.isAbsolute())
			throw new JsonQueryException("import path must be relative: " + path);

		for (Path component : modulePath) {
			if ("..".equals(component.toString()))
				throw new JsonQueryException("import path must not traverse to parent directories: " + path);
		}

		if (modulePath.getParent() != null && modulePath.getFileName().equals(modulePath.getParent().getFileName()))
			throw new JsonQueryException("module names must not have equal consecutive components: " + path);

		Path resolvedPath = base.resolve(modulePath).normalize();
		// A module is a file *under* the directory, never the directory itself: findFile looks at a
		// sibling of what it is given, and the sibling of the directory itself lies outside it.
		if (!resolvedPath.startsWith(base) || resolvedPath.equals(base))
			throw new JsonQueryException("import path must be within the search path: " + path);

		return resolvedPath;
	}

	/**
	 * jq looks for a module both as {@code <name>.<ext>} and as {@code <name>/<name>.<ext>}. The
	 * first of those is a *sibling* of the resolved path, so both candidates are checked against
	 * {@code searchPath} before the filesystem is touched.
	 */
	private static @Nullable Path findFile(Path searchPath, Path resolvedPath, String ext) {
		Path fileName = resolvedPath.getFileName();
		if (fileName == null)
			return null;

		Path siblingPath = resolvedPath.resolveSibling(fileName + "." + ext);
		if (isReadableWithin(searchPath, siblingPath))
			return siblingPath;

		Path nestedPath = resolvedPath.resolve(fileName + "." + ext);
		if (isReadableWithin(searchPath, nestedPath))
			return nestedPath;

		return null;
	}

	/**
	 * The last word on what this loader may open. Every path it resolves is bounded before it gets
	 * here, so this is the backstop that keeps a future slip in that arithmetic from turning into a
	 * read outside the search path.
	 */
	private static boolean isReadableWithin(Path searchPath, Path filePath) {
		return filePath.normalize().startsWith(searchPath) && Files.isReadable(filePath);
	}

	private static String read(Path filePath, String what, String path) throws JsonQueryException {
		try {
			return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		} catch (FileNotFoundException | NoSuchFileException e) {
			// Readable a moment ago, gone now: report it as missing rather than as a read failure.
			throw new ModuleNotFoundException(path, e);
		} catch (IOException e) {
			throw new JsonQueryException(String.format("failed to load %s %s: %s", what, path, e.getMessage()), e);
		}
	}

	private static <JsonNode> JsonNode parseData(JsonProvider<JsonNode> jsonProvider, Path filePath, String path) throws JsonQueryException {
		List<JsonNode> values = jsonProvider.parseAll(read(filePath, "data", path));
		return jsonProvider.createArray(values);
	}

	@Override
	public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		for (Path searchPath : searchPaths) {
			Path filePath = findFile(searchPath, resolveModulePath(searchPath, path), "jq");
			if (filePath != null)
				return new FileSystemJqModule<>(searchPath, filePath, read(filePath, "module", path), jsonProvider);
		}
		throw new ModuleNotFoundException(path);
	}

	@Override
	public JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		for (Path searchPath : searchPaths) {
			Path filePath = findFile(searchPath, resolveModulePath(searchPath, path), "json");
			if (filePath != null)
				return parseData(jsonProvider, filePath, path);
		}
		throw new ModuleNotFoundException(path);
	}
}
