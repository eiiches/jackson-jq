package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModule;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModuleMeta;
import net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class FileSystemModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final List<Path> searchPaths;
	private final Version version;
	private final JsonProvider<JsonNode> jsonProvider;
	private final @Nullable ModuleLoader<JsonNode> parentModuleLoader;

	public FileSystemModuleLoader(JsonProvider<JsonNode> jsonProvider, Version version, Path... searchPaths) {
		this(jsonProvider, null, version, searchPaths);
	}

	public FileSystemModuleLoader(JsonProvider<JsonNode> jsonProvider, @Nullable ModuleLoader<JsonNode> parentModuleLoader, Version version, Path... searchPaths) {
		List<Path> absoluteSearchPaths = new ArrayList<>();
		for (Path searchPath : searchPaths) {
			if (!searchPath.isAbsolute())
				throw new RuntimeException("Search path must be absolute");
			absoluteSearchPaths.add(searchPath);
		}
		this.searchPaths = absoluteSearchPaths;
		this.jsonProvider = jsonProvider;
		this.parentModuleLoader = parentModuleLoader;
		this.version = version;
	}

	private static Path resolveModulePath(Path searchPath, String path) {
		Path modulePath = searchPath.getFileSystem().getPath(path);
		if (modulePath.isAbsolute())
			throw new RuntimeException("Import path must be relative");

		if (modulePath.getParent() != null && modulePath.getFileName().equals(modulePath.getParent().getFileName()))
			throw new RuntimeException("module names must not have equal consecutive components: " + path);

		Path resolvedPath = searchPath.resolve(modulePath).normalize();
		if (!resolvedPath.startsWith(searchPath))
			throw new RuntimeException("Import path must be within the search path");

		return resolvedPath;
	}

	private static @Nullable ModuleFile loadModuleFile(Path searchPath, String path, String ext) throws IOException {
		Path resolvedPath = resolveModulePath(searchPath, path);

		Path moduleFilePath = resolvedPath.resolveSibling(resolvedPath.getFileName() + "." + ext);
		try {
			byte[] moduleBytes = Files.readAllBytes(moduleFilePath);
			return new ModuleFile(searchPath, moduleFilePath, moduleBytes);
		} catch (FileNotFoundException | NoSuchFileException e) {
			/* continue */
		}

		Path moduleFilePath2 = resolvedPath.resolve(resolvedPath.getFileName() + "." + ext);
		try {
			byte[] moduleBytes = Files.readAllBytes(moduleFilePath2);
			return new ModuleFile(searchPath, moduleFilePath2, moduleBytes);
		} catch (FileNotFoundException | NoSuchFileException e) {
			/* continue */
		}

		return null;
	}

	private static final class ModuleFile {
		Path searchPath;
		Path modulePath;
		byte[] bytes;

		ModuleFile(Path searchPath, Path modulePath, byte[] bytes) {
			this.searchPath = searchPath;
			this.modulePath = modulePath;
			this.bytes = bytes;
		}
	}

	// modules with the same path may exist in different search paths
	private final ConcurrentHashMap<Pair<Path /* searchPath */, String /* relativePath */>, TryOnce<Module>> loadedModules = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Pair<Path /* searchPath */, String /* relativePath */>, TryOnce<JsonNode>> loadedData = new ConcurrentHashMap<>();

	private final class FileSystemModule extends SimpleModule {
		private Path modulePath;
		private Path searchPath;

		FileSystemModule(Path searchPath, Path modulePath) {
			this.modulePath = modulePath;
			this.searchPath = searchPath;
		}

		private FileSystemModuleLoader<JsonNode> loader() {
			return FileSystemModuleLoader.this;
		}
	}

	private @Nullable Module loadModuleActual(Path searchPath, String path) throws IOException {
		ModuleFile moduleFile = loadModuleFile(searchPath, path, "jq");
		if (moduleFile == null)
			return null;

		String moduleString = new String(moduleFile.bytes, StandardCharsets.UTF_8);

		FileSystemModule module = new FileSystemModule(moduleFile.searchPath, moduleFile.modulePath);

		Environment<JsonNode> moduleEnv = new EnvironmentBuilder<>(jsonProvider, version)
				.setModuleLoader(parentModuleLoader != null ? parentModuleLoader : this)
				.build();
		AstNode ast = AstParser.parse(moduleString + " null", version);
		Expression<StackFrame, JsonNode> compiled = Compiler.compileModule(moduleEnv, module, ast);
		if (!(compiled instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");

		Map<FunctionSignature, Function> exportedFunctions = ((RootExpression<JsonNode>) compiled).applyForModuleExports(jsonProvider.createNull());
		exportedFunctions.forEach((key, factory) -> {
			if (key.arity() != null)
				module.addFunction(key, factory);
		});
		module.setModuleMeta(SimpleModuleMeta.fromAst(ast));
		return module;
	}

	private static final class TryOnce<T> {
		private CompletableFuture<T> f = new CompletableFuture<>();
		private Thread taskThread;

		@Var
		private boolean taskStarted;

		TryOnce() {
			this.taskThread = Thread.currentThread();
		}

		private static class RecursiveInvocationException extends IllegalStateException {
			private static final long serialVersionUID = 1L;
		}

		T tryOnce(Callable<T> task) throws CompletionException, RecursiveInvocationException {
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

	private @Nullable Pair<List<Path>, String> resolvePathsFromImportDirective(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
		@Var List<Path> searchPaths = this.searchPaths;
		@Var String relativePath = path;

		@Var FileSystemModule callerModule = null;
		if (caller != null && caller.getClass() == FileSystemModule.class) {
			callerModule = (FileSystemModule) caller;
			if (callerModule.loader() != this) // Imports from a FileSystemModule should be handled by the same loader
				return null;
		}

		JsonProvider<JsonNode> jsonProvider = this.jsonProvider;
		if (metadata != null) {
			JsonNode search = jsonProvider.getObjectMember(metadata, "search");
			if (search != null) {
				// disallow search overrides from top-level unnamed expression, which doesn't have a module path.
				// i.e. import "foo" as foo {search: ./}; doesn't make sense. where is ./ ?
				if (callerModule == null)
					throw new JsonQueryException("search path can only be overriden from imported modules, but not from a top-level unnamed module");

				// jq does ignore non-textual search overrides, but i want it to fail fast.
				if (!jsonProvider.isString(search))
					throw new JsonQueryException("search path overrides must be a string");

				@Var Path searchPathOverride = callerModule.modulePath.getFileSystem().getPath(jsonProvider.getString(search));
				searchPathOverride = Objects.requireNonNull(callerModule.modulePath.getParent()).resolve(searchPathOverride).normalize();

				// still, the search path must be within the original search path
				if (!searchPathOverride.startsWith(callerModule.searchPath))
					throw new JsonQueryException("search path overrides from import metadata must stay within the original search path of the caller module");

				Path resolvedModulePath = resolveModulePath(searchPathOverride, path);

				relativePath = callerModule.searchPath.relativize(resolvedModulePath).toString();
				searchPaths = Collections.singletonList(callerModule.searchPath);
			}
		}

		return Pair.of(searchPaths, relativePath);
	}

	@Override
	public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
		Pair<List<Path>, String> paths = resolvePathsFromImportDirective(caller, path, metadata);
		if (paths == null)
			return null;
		List<Path> searchPaths = paths._1;
		String relativePath = paths._2;

		for (Path searchPath : searchPaths) {
			TryOnce<Module> tryOnce = loadedModules.computeIfAbsent(Pair.of(searchPath, relativePath), p -> new TryOnce<>());
			try {
				Module module = tryOnce.tryOnce(() -> {
					return loadModuleActual(searchPath, relativePath);
				});
				if (module != null)
					return module;
			} catch (TryOnce.RecursiveInvocationException e) {
				throw new JsonQueryException(String.format("module %s is imported recursively", path));
			} catch (CompletionException e) {
				Throwable cause = e.getCause();
				throw new JsonQueryException(String.format("failed to load module %s: %s", path, cause == null ? e.getMessage() : cause.getMessage()), e);
			}
		}

		return null;
	}

	@Override
	public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
		Pair<List<Path>, String> paths = resolvePathsFromImportDirective(caller, path, metadata);
		if (paths == null)
			return null;
		List<Path> searchPaths = paths._1;
		String relativePath = paths._2;

		for (Path searchPath : searchPaths) {
			TryOnce<JsonNode> tryOnce = loadedData.computeIfAbsent(Pair.of(searchPath, relativePath), p -> new TryOnce<>());
			try {
				JsonNode data = tryOnce.tryOnce(() -> {
					return loadDataActual(searchPath, relativePath);
				});
				if (data != null)
					return data;
			} catch (CompletionException e) {
				Throwable cause = e.getCause();
				throw new JsonQueryException(String.format("failed to load data %s: %s", path, cause == null ? e.getMessage() : cause.getMessage()), e);
			}
		}

		return null;
	}

	private @Nullable JsonNode loadDataActual(Path searchPath, String path) throws IOException {
		ModuleFile moduleFile = loadModuleFile(searchPath, path, "json");
		if (moduleFile == null)
			return null;

		JsonProvider<JsonNode> jsonProvider = this.jsonProvider;
		List<JsonNode> values = jsonProvider.parseAll(new String(moduleFile.bytes, StandardCharsets.UTF_8));
		return jsonProvider.createArray(values);
	}
}
