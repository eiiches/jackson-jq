package net.thisptr.jackson.jq.module.loaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.module.SimpleModule;

public class FileSystemModuleLoader implements ModuleLoader {
	private final List<Path> searchPaths;
	private final Version version;
	private final Scope parentScope;

	public FileSystemModuleLoader(final Scope parentScope, final Version version, final Path... searchPaths) {
		final List<Path> absoluteSearchPaths = new ArrayList<>();
		for (final Path searchPath : searchPaths) {
			if (!searchPath.isAbsolute())
				throw new RuntimeException("Search path must be absolute");
			absoluteSearchPaths.add(searchPath);
		}
		this.searchPaths = absoluteSearchPaths;
		this.parentScope = parentScope;
		this.version = version;
	}

	private static final Path resolveModulePath(final Path searchPath, final String path) {
		final Path modulePath = searchPath.getFileSystem().getPath(path);
		if (modulePath.isAbsolute())
			throw new RuntimeException("Import path must be relative");

		if (modulePath.getParent() != null && modulePath.getFileName().equals(modulePath.getParent().getFileName()))
			throw new RuntimeException("module names must not have equal consecutive components: " + path);

		final Path resolvedPath = searchPath.resolve(modulePath).normalize();
		if (!resolvedPath.startsWith(searchPath))
			throw new RuntimeException("Import path must be within the search path");

		return resolvedPath;
	}

	private static ModuleFile loadModuleFile(final Path searchPath, final String path, final String ext) throws IOException {
		final Path resolvedPath = resolveModulePath(searchPath, path);

		final Path moduleFilePath = resolvedPath.resolveSibling(resolvedPath.getFileName() + "." + ext);
		try {
			final byte[] moduleBytes = Files.readAllBytes(moduleFilePath);
			return new ModuleFile(searchPath, moduleFilePath, moduleBytes);
		} catch (FileNotFoundException | NoSuchFileException e) {
			/* continue */
		}

		final Path moduleFilePath2 = resolvedPath.resolve(resolvedPath.getFileName() + "." + ext);
		try {
			final byte[] moduleBytes = Files.readAllBytes(moduleFilePath2);
			return new ModuleFile(searchPath, moduleFilePath2, moduleBytes);
		} catch (FileNotFoundException | NoSuchFileException e) {
			/* continue */
		}

		return null;
	}

	private static final class ModuleFile {
		public final Path searchPath;
		public final Path modulePath;
		public final byte[] bytes;

		public ModuleFile(Path searchPath, Path modulePath, byte[] bytes) {
			this.searchPath = searchPath;
			this.modulePath = modulePath;
			this.bytes = bytes;
		}
	}

	// modules with the same path may exist in different search paths
	private final ConcurrentHashMap<Pair<Path /* searchPath */, String /* relativePath */>, TryOnce<Module>> loadedModules = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Pair<Path /* searchPath */, String /* relativePath */>, TryOnce<JsonNode>> loadedData = new ConcurrentHashMap<>();

	private static final class FileSystemModule extends SimpleModule {
		private final Path modulePath;
		private final Path searchPath;
		private final FileSystemModuleLoader loader;

		public FileSystemModule(final FileSystemModuleLoader loader, final Path searchPath, final Path modulePath) {
			this.loader = loader;
			this.modulePath = modulePath;
			this.searchPath = searchPath;
		}
	}

	private Module loadModuleActual(final Path searchPath, final String path) throws IOException {
		final ModuleFile moduleFile = loadModuleFile(searchPath, path, "jq");
		if (moduleFile == null)
			return null;

		final String moduleString = new String(moduleFile.bytes, StandardCharsets.UTF_8);

		final FileSystemModule module = new FileSystemModule(this, moduleFile.searchPath, moduleFile.modulePath);

		final Scope childScope = Scope.newChildScope(parentScope);
		childScope.setCurrentModule(module);

		// TODO: use different parser instead of adding null at the end
		final Expression expr = ExpressionParser.compile(moduleString + " null", version);
		expr.apply(childScope, NullNode.getInstance(), null, (o, p) -> {}, false);

		module.addAllFunctions(childScope.getLocalFunctions());
		return module;
	}

	private static final class TryOnce<T> {
		private final CompletableFuture<T> f = new CompletableFuture<>();
		private final Thread taskThread;

		private boolean taskStarted;

		public TryOnce() {
			this.taskThread = Thread.currentThread();
		}

		private static final class RecursiveInvocationException extends IllegalStateException {
			private static final long serialVersionUID = 1L;
		}

		public T tryOnce(Callable<T> task) throws CompletionException, RecursiveInvocationException {
			if (f.isDone())
				return f.join();

			if (Thread.currentThread() == taskThread) {
				// if task is already started BUT not completed, tryOnce is being called recursively
				if (taskStarted)
					throw new RecursiveInvocationException();
				taskStarted = true;

				// perform the task
				try {
					f.complete(task.call());
				} catch (Throwable th) {
					f.completeExceptionally(th);
				}

				return f.join(); // return the result we just computed
			}

			// wait for the task thread to complete
			return f.join();
		}
	}

	private Pair<List<Path>, String> resolvePathsFromImportDirective(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		List<Path> searchPaths = this.searchPaths;
		String relativePath = path;

		FileSystemModule callerModule = null;
		if (caller instanceof FileSystemModule) { // implies caller != null
			callerModule = (FileSystemModule) caller;
			if (callerModule.loader != this) // Imports from a FileSystemModule should be handled by the same loader
				return null;
		}

		if (metadata != null) {
			final JsonNode search = metadata.get("search");
			if (search != null) {
				// disallow search overrides from top-level unnamed expression, which doesn't have a module path.
				// i.e. import "foo" as foo {search: ./}; doesn't make sense. where is ./ ?
				if (callerModule == null)
					throw new JsonQueryException("search path can only be overriden from imported modules, but not from a top-level unnamed module");

				// jq does ignore non-textual search overrides, but i want it to fail fast.
				if (!search.isTextual())
					throw new JsonQueryException("search path overrides must be a string");

				Path searchPathOverride = callerModule.modulePath.getFileSystem().getPath(search.asText());
				searchPathOverride = callerModule.modulePath.getParent().resolve(searchPathOverride).normalize();

				// still, the search path must be within the original search path
				if (!searchPathOverride.startsWith(callerModule.searchPath))
					throw new JsonQueryException("search path overrides from import metadata must stay within the original search path of the caller module");

				final Path resolvedModulePath = resolveModulePath(searchPathOverride, path);

				relativePath = callerModule.searchPath.relativize(resolvedModulePath).toString();
				searchPaths = Collections.singletonList(callerModule.searchPath);
			}
		}

		return Pair.of(searchPaths, relativePath);
	}

	@Override
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		final Pair<List<Path>, String> paths = resolvePathsFromImportDirective(caller, path, metadata);
		if (paths == null)
			return null;
		final List<Path> searchPaths = paths._1;
		final String relativePath = paths._2;

		for (final Path searchPath : searchPaths) {
			final TryOnce<Module> tryOnce = loadedModules.computeIfAbsent(Pair.of(searchPath, relativePath), p -> new TryOnce<>());
			try {
				final Module module = tryOnce.tryOnce(() -> {
					return loadModuleActual(searchPath, relativePath);
				});
				if (module != null)
					return module;
			} catch (TryOnce.RecursiveInvocationException e) {
				throw new JsonQueryException("module %s is imported recursively", path);
			} catch (CompletionException e) {
				throw new JsonQueryException(String.format("failed to load module %s: %s", path, e.getCause().getMessage()), e);
			}
		}

		return null;
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		final Pair<List<Path>, String> paths = resolvePathsFromImportDirective(caller, path, metadata);
		if (paths == null)
			return null;
		final List<Path> searchPaths = paths._1;
		final String relativePath = paths._2;

		for (final Path searchPath : searchPaths) {
			final TryOnce<JsonNode> tryOnce = loadedData.computeIfAbsent(Pair.of(searchPath, relativePath), p -> new TryOnce<>());
			try {
				final JsonNode data = tryOnce.tryOnce(() -> {
					return loadDataActual(searchPath, relativePath);
				});
				if (data != null)
					return data;
			} catch (CompletionException e) {
				throw new JsonQueryException(String.format("failed to load data %s: %s", path, e.getCause().getMessage()), e);
			}
		}

		return null;
	}

	private JsonNode loadDataActual(final Path searchPath, final String path) throws IOException {
		final ModuleFile moduleFile = loadModuleFile(searchPath, path, "json");
		if (moduleFile == null)
			return null;

		final ArrayNode data = MAPPER.createArrayNode();

		final MappingIterator<JsonNode> iter = MAPPER.readValues(MAPPER.getFactory().createParser(moduleFile.bytes), JsonNode.class);
		while (iter.hasNext())
			data.add(iter.next());

		return data;
	}
}
