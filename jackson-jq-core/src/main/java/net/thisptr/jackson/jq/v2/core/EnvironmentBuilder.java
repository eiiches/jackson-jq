package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.env.EnvironmentImpl;
import net.thisptr.jackson.jq.v2.core.internal.function.loaders.CachedFunctionLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

/**
 * Fluent builder for {@link Environment}. {@code Environment} itself is a read-only view; all
 * configuration happens here, before {@link #build()}.
 */
public final class EnvironmentBuilder<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version jqVersion;

	private final List<ModuleLoader<JsonNode>> moduleLoaders = new ArrayList<>();
	private final List<FunctionLoader> functionLoaders = new ArrayList<>();

	private final Set<String> declaredVariables = new HashSet<>();
	private final Set<FunctionSignature> declaredFunctions = new HashSet<>();
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private final Map<FunctionSignature, JqFunction> jqFunctions = new HashMap<>();
	private final Map<String, JsonNode> constants = new HashMap<>();
	private final Map<String, Module> importedModules = new HashMap<>();

	private EnvironmentBuilder(JsonProvider<JsonNode> jsonProvider, Version jqVersion) {
		this.jsonProvider = jsonProvider;
		this.jqVersion = jqVersion;
	}

	/**
	 * Starts a builder that already knows how to find what is on the classpath: a
	 * {@link ClassPathModuleLoader} for {@code import}ed modules, and a {@link ClassPathFunctionLoader}
	 * for functions -- which is where the jq builtins come from. Drop either with
	 * {@link #clearModuleLoaders()} or {@link #clearFunctionLoaders()}.
	 * <p>
	 * Both discover their providers through this class's own {@link ClassLoader}. Where that is not
	 * the one that can see the application's providers -- an OSGi bundle, a JPMS layer, a plugin
	 * class loader -- name the right one with {@link #withDefaultLoaders(JsonProvider, Version, ClassLoader)}.
	 */
	public static <JsonNode> EnvironmentBuilder<JsonNode> withDefaultLoaders(JsonProvider<JsonNode> jsonProvider, Version jqVersion) {
		return withDefaultLoaders(jsonProvider, jqVersion, EnvironmentBuilder.class.getClassLoader());
	}

	/**
	 * Same as {@link #withDefaultLoaders(JsonProvider, Version)}, but both default loaders discover
	 * their providers through {@code classLoader} instead of this class's own.
	 *
	 * @param classLoader the class loader both {@link ClassPathModuleLoader} and
	 * {@link ClassPathFunctionLoader} search for providers
	 */
	public static <JsonNode> EnvironmentBuilder<JsonNode> withDefaultLoaders(JsonProvider<JsonNode> jsonProvider, Version jqVersion, ClassLoader classLoader) {
		EnvironmentBuilder<JsonNode> builder = new EnvironmentBuilder<>(Objects.requireNonNull(jsonProvider, "jsonProvider"), Objects.requireNonNull(jqVersion, "jqVersion"));
		builder.addModuleLoader(new ClassPathModuleLoader<>(classLoader));
		builder.addFunctionLoader(new ClassPathFunctionLoader(classLoader));
		return builder;
	}

	public JsonProvider<JsonNode> getJsonProvider() {
		return jsonProvider;
	}

	public Version getJqVersion() {
		return jqVersion;
	}

	/**
	 * Appends a loader to the ones this environment consults. They are asked in the order they were
	 * added, and the first one to resolve an {@code import}/{@code include} path answers it; a loader
	 * that resolved the path and then failed aborts the search rather than deferring to the next.
	 */
	public EnvironmentBuilder<JsonNode> addModuleLoader(ModuleLoader<JsonNode> moduleLoader) {
		moduleLoaders.add(Objects.requireNonNull(moduleLoader, "moduleLoader"));
		return this;
	}

	/**
	 * Removes every module loader added so far, including the default one
	 * {@link #withDefaultLoaders} installed -- the way to take over the search order completely.
	 * An environment left with no module loaders fails every {@code import} and {@code include} with
	 * {@code ModuleNotFoundException}.
	 */
	public EnvironmentBuilder<JsonNode> clearModuleLoaders() {
		moduleLoaders.clear();
		return this;
	}

	/**
	 * Appends a loader to the ones this environment consults. They are asked in the order they were
	 * added, and the first one to supply the called signature answers it -- so a loader added after
	 * the default {@link ClassPathFunctionLoader} extends the builtins rather than shadowing them.
	 * To shadow a name, define it on the environment itself with {@link #defineFunction} or
	 * {@link #defineJqFunction}, which beat every loader, or take over the search order with
	 * {@link #clearFunctionLoaders()}.
	 */
	public EnvironmentBuilder<JsonNode> addFunctionLoader(FunctionLoader functionLoader) {
		Objects.requireNonNull(functionLoader, "functionLoader");
		// A loader is asked for a whole registry on every call it has to resolve, and discovering one
		// can be expensive, so each gets memoized per jq version for as long as this environment lives.
		// Already-memoized loaders -- what Environment.getFunctionLoaders() hands back, which is how
		// the compiler passes a query's loaders down to an imported module -- are taken as they are,
		// so that the module shares the caller's caches instead of starting cold behind a second layer.
		functionLoaders.add(functionLoader instanceof CachedFunctionLoader ? functionLoader : new CachedFunctionLoader(functionLoader));
		return this;
	}

	/**
	 * Removes every function loader added so far, including the default one
	 * {@link #withDefaultLoaders} installed -- the way to take over the search order completely.
	 * An environment left with no function loaders has no jq builtins: every call it cannot resolve
	 * from its own definitions fails with "Function name/arity does not exist".
	 */
	public EnvironmentBuilder<JsonNode> clearFunctionLoaders() {
		functionLoaders.clear();
		return this;
	}

	/**
	 * Declares {@code name} as a valid global variable with no value -- every query that references it
	 * must be supplied a value via {@link RuntimeBindings} on every {@code apply()} call, or that call
	 * fails immediately.
	 */
	public EnvironmentBuilder<JsonNode> declareVariable(String name) {
		requireUnusedVariableName(name);
		declaredVariables.add(name);
		return this;
	}

	/**
	 * Declares {@code signature} as a valid global function with no implementation -- every query that
	 * calls it must be supplied an implementation via {@link RuntimeBindings} on every {@code apply()}
	 * call, or that call fails immediately.
	 */
	public EnvironmentBuilder<JsonNode> declareFunction(FunctionSignature signature) {
		requireUnusedFunctionSignature(signature);
		declaredFunctions.add(signature);
		return this;
	}

	/**
	 * Defines {@code name} with a fixed {@code supplier}, evaluated on every reference. This value is
	 * baked into the compiled query and can never be overridden by {@link RuntimeBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineVariable(String name, Supplier<JsonNode> supplier) {
		requireUnusedVariableName(name);
		variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(supplier, "supplier"));
		return this;
	}

	/**
	 * Defines {@code name} with a fixed {@code value}. Like {@link #defineVariable}, this can never be
	 * overridden by {@link RuntimeBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineConstant(String name, JsonNode value) {
		requireUnusedVariableName(name);
		constants.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
		return this;
	}

	/**
	 * Defines {@code nameAndArity} with a fixed {@code function}. Like {@link #defineVariable}, this can
	 * never be overridden by {@link RuntimeBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineFunction(FunctionSignature nameAndArity, Function function) {
		requireUnusedFunctionSignature(nameAndArity);
		functions.put(nameAndArity, function);
		return this;
	}

	/**
	 * Defines a jq function whose exact signature is derived from its name and parameter count. The
	 * function is compiled against the completed environment and can never be overridden by
	 * {@link RuntimeBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineJqFunction(JqFunction function) {
		Objects.requireNonNull(function, "function");
		FunctionSignature signature = function.signature();
		requireUnusedFunctionSignature(signature);
		VersionRange version = function.version();
		if (version != null && !version.contains(jqVersion))
			throw new IllegalArgumentException("Function " + signature + " does not support jq " + jqVersion);
		jqFunctions.put(signature, function);
		return this;
	}

	private void requireUnusedVariableName(String name) {
		Objects.requireNonNull(name, "name");
		if (declaredVariables.contains(name))
			throw new IllegalArgumentException("Variable $" + name + " was already declared via declareVariable()");
		if (variables.containsKey(name))
			throw new IllegalArgumentException("Variable $" + name + " was already defined via defineVariable()");
		if (constants.containsKey(name))
			throw new IllegalArgumentException("Variable $" + name + " was already defined via defineConstant()");
	}

	private void requireUnusedFunctionSignature(FunctionSignature signature) {
		Objects.requireNonNull(signature, "signature");
		if (declaredFunctions.contains(signature))
			throw new IllegalArgumentException("Function " + signature + " was already declared via declareFunction()");
		if (functions.containsKey(signature))
			throw new IllegalArgumentException("Function " + signature + " was already defined via defineFunction()");
		if (jqFunctions.containsKey(signature))
			throw new IllegalArgumentException("Function " + signature + " was already defined via defineJqFunction()");
	}

	/**
	 * Makes {@code module}'s functions callable as {@code name::func(...)} without the query having
	 * to {@code import} it. A {@link JavaModule} is used as it stands, a
	 * {@link net.thisptr.jackson.jq.v2.spi.module.JqModule} is compiled the first time a query
	 * actually calls into it, and a module implementing both contributes both sets of functions.
	 */
	public EnvironmentBuilder<JsonNode> addImportedModule(String name, Module module) {
		importedModules.put(name, module);
		return this;
	}

	public Environment<JsonNode> build() {
		return new EnvironmentImpl<>(jsonProvider, jqVersion, moduleLoaders, functionLoaders,
				declaredVariables, declaredFunctions, variables, functions, jqFunctions, constants, importedModules);
	}
}
