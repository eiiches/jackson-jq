package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModule;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModuleMeta;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Turns the paths in {@code import}/{@code include} statements into usable modules: asks the
 * environment's {@link ModuleLoader}s for them, compiles the ones that come back as jq source, and
 * remembers what it has compiled.
 * <p>
 * One resolver serves one compilation. Everything it remembers -- which module compiled to what,
 * which modules are still being compiled -- lasts exactly as long as that, so it is reached from a
 * single thread and can never hand a later compilation something resolved against a different set
 * of loaders. Loaders themselves stay stateless: they read, they do not compile and they do not
 * cache.
 */
public final class ModuleResolver<JsonNode> {
	private final Environment<JsonNode> env;

	/**
	 * Compiled modules. Two imports reaching the same module compile it once.
	 */
	private final Map<JqModule<JsonNode>, JavaModule> compiled = new HashMap<>();

	/**
	 * Modules being compiled right now -- an import of one of these is a cycle.
	 */
	private final Set<JqModule<JsonNode>> compiling = new HashSet<>();

	private @Nullable Environment<JsonNode> moduleEnv;

	public ModuleResolver(Environment<JsonNode> env) {
		this.env = env;
	}

	/**
	 * Resolves an {@code import}/{@code include} path to a module whose functions can be called.
	 *
	 * @param origin the module the import statement appears in, or {@code null} at the top level
	 */
	public JavaModule resolveModule(@Nullable JqModule<JsonNode> origin, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		Maybe<JsonNode> search = searchOverride(origin, path, metadata);
		if (search.isPresent())
			return compile(requireNonNull(origin).relativeImport(path, env.getJsonProvider().getString(search.get())));

		@Var Module module = null;
		for (ModuleLoader<JsonNode> loader : env.getModuleLoaders()) {
			try {
				module = loader.loadModule(path, metadata);
				break;
			} catch (ModuleNotFoundException e) {
				/* this loader doesn't have it; try the next one */
			}
		}
		if (module == null)
			throw new ModuleNotFoundException(path);
		return materialize(module);
	}

	/**
	 * Resolves an {@code import path as $NAME} path to its data.
	 *
	 * @param origin the module the import statement appears in, or {@code null} at the top level
	 */
	public JsonNode resolveData(@Nullable JqModule<JsonNode> origin, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		Maybe<JsonNode> search = searchOverride(origin, path, metadata);
		if (search.isPresent())
			return requireNonNull(origin).relativeData(path, env.getJsonProvider().getString(search.get()));

		for (ModuleLoader<JsonNode> loader : env.getModuleLoaders()) {
			try {
				return loader.loadData(path, metadata);
			} catch (ModuleNotFoundException e) {
				/* this loader doesn't have it; try the next one */
			}
		}
		throw new ModuleNotFoundException(path);
	}

	/**
	 * Makes a module usable: a {@link JavaModule} already is, a {@link JqModule} is compiled, and a
	 * module implementing both has its Java and jq functions combined.
	 * <p>
	 * Public because an {@code Environment} may be handed either kind, or a hybrid of both, through
	 * {@code addImportedModule}, and which one it got only matters here.
	 */
	public JavaModule materialize(Module module) throws JsonQueryException {
		if (module instanceof JqModule<?> jqModule) {
			@SuppressWarnings("unchecked") // A loader of ours produced it, so its node type is ours.
			JqModule<JsonNode> typed = (JqModule<JsonNode>) jqModule;
			return compile(typed);
		}
		if (module instanceof JavaModule javaModule)
			return javaModule;
		throw new JsonQueryException(String.format("module %s is neither a JqModule nor a JavaModule", module.getClass().getName()));
	}

	/**
	 * An import is relative exactly when it carries a {@code search} override, which only means
	 * something inside a module: there is nothing for a top-level script to be relative to. The
	 * value has to be a string -- jq ignores a non-textual one, we would rather say so.
	 */
	private Maybe<JsonNode> searchOverride(@Nullable JqModule<JsonNode> origin, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = env.getJsonProvider();
		Maybe<JsonNode> search = metadata.isPresent() ? jsonProvider.getObjectMember(metadata.get(), "search") : Maybe.absent();
		if (!search.isPresent()) {
			// A leading "./" in an import path is inert in jq -- only a search override is relative
			// -- and an absolute one is refused outright. See docs/jq-module-observations.md.
			if (path.startsWith("/"))
				throw new JsonQueryException("import path must be relative: " + path);
			return Maybe.absent();
		}
		if (!jsonProvider.isString(search.get()))
			throw new JsonQueryException("search path overrides must be a string");
		if (origin == null)
			throw new JsonQueryException("search path can only be overriden from imported modules, but not from a top-level unnamed module");
		return search;
	}

	private static <JsonNode> JqModule<JsonNode> requireNonNull(@Nullable JqModule<JsonNode> origin) {
		if (origin == null)
			throw new IllegalStateException("a relative import without an origin should have been rejected already");
		return origin;
	}

	/**
	 * Compiles a module's source, having first resolved its own imports -- which is where this
	 * recurses, and where a cycle shows up.
	 */
	private JavaModule compile(JqModule<JsonNode> module) throws JsonQueryException {
		JavaModule alreadyCompiled = compiled.get(module);
		if (alreadyCompiled != null)
			return alreadyCompiled;
		if (!compiling.add(module))
			throw new JsonQueryException(String.format("module %s is imported recursively", module));

		try {
			Map<FunctionSignature, Function> javaFunctions = module instanceof JavaModule javaModule
					? javaModule.getFunctions()
					: Collections.emptyMap();
			// A module off a search path is somebody else's library, so it is compiled with default
			// options -- the caller asked for diagnostics about their own query, not about the jq
			// files it happens to import.
			JavaModule result = compileSource(moduleEnvironment(javaFunctions), CompileOptions.newBuilder().build(), new ModuleScope<>(this, module), module, javaFunctions);
			compiled.put(module, result);
			return result;
		} finally {
			compiling.remove(module);
		}
	}

	/**
	 * Compiles jq source into a module: runs it once so every exported {@code def} lands in its slot
	 * with its closures bound, then reads those out.
	 */
	private static <JsonNode> JavaModule compileSource(Environment<JsonNode> env, CompileOptions options, ModuleScope<JsonNode> scope, JqModule<JsonNode> sourceModule, Map<FunctionSignature, Function> javaFunctions) throws JsonQueryException {
		AstNode ast = AstParser.parse(sourceModule.getSourceCode() + " null", env.getJqVersion());
		Expression<StackFrame, JsonNode> compiled = Compiler.compileModule(env, options, scope, ast);
		if (!(compiled instanceof RootExpression<JsonNode> rootExpr))
			throw new IllegalStateException("Compiler did not produce a root expression");

		SimpleModule module = new SimpleModule();
		module.addAllFunctions(javaFunctions);
		Map<FunctionSignature, Function> exportedFunctions = rootExpr.applyForModuleExports(env.getJsonProvider().createNull());
		exportedFunctions.forEach((key, factory) -> {
			if (key.arity() != null) {
				if (javaFunctions.containsKey(key))
					throw new JsonQueryException(String.format("module %s defines function %s in both Java and jq source", sourceModule, key));
				module.addFunction(key, factory);
			}
		});
		module.setModuleMeta(SimpleModuleMeta.fromAst(ast));
		return module;
	}

	/**
	 * The environment an imported module compiles against: the importing environment's JSON
	 * provider, jq version and function loaders -- so a custom {@code FunctionLoader} reaches modules
	 * too, in the same order -- but none of its globals, which belong to the query, not to the library
	 * it imports. It needs no module loaders: this resolver, not that environment, resolves the
	 * module's imports.
	 */
	private Environment<JsonNode> moduleEnvironment(Map<FunctionSignature, Function> moduleFunctions) {
		if (!moduleFunctions.isEmpty()) {
			EnvironmentBuilder<JsonNode> builder = newModuleEnvironmentBuilder();
			moduleFunctions.forEach(builder::defineFunction);
			return builder.build();
		}

		@Var
		Environment<JsonNode> cached = moduleEnv;
		if (cached == null) {
			cached = newModuleEnvironmentBuilder().build();
			moduleEnv = cached;
		}
		return cached;
	}

	private EnvironmentBuilder<JsonNode> newModuleEnvironmentBuilder() {
		EnvironmentBuilder<JsonNode> builder = EnvironmentBuilder.withDefaultLoaders(env.getJsonProvider(), env.getJqVersion())
				.clearModuleLoaders()
				.clearFunctionLoaders();
		env.getFunctionLoaders().forEach(builder::addFunctionLoader);
		return builder;
	}
}
