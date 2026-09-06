package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.internal.CachedFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.env.EnvironmentImpl;
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

	private ModuleLoader<JsonNode> moduleLoader = new ClassPathModuleLoader<>(EnvironmentBuilder.class.getClassLoader());
	private FunctionLoader functionLoader = new CachedFunctionLoader(new ClassPathFunctionLoader(EnvironmentBuilder.class.getClassLoader()));

	private final Set<String> declaredVariables = new HashSet<>();
	private final Set<FunctionSignature> declaredFunctions = new HashSet<>();
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private final Map<FunctionSignature, JqFunction> jqFunctions = new HashMap<>();
	private final Map<String, JsonNode> constants = new HashMap<>();
	private final Map<String, Module> importedModules = new HashMap<>();

	public EnvironmentBuilder(JsonProvider<JsonNode> jsonProvider, Version jqVersion) {
		this.jsonProvider = jsonProvider;
		this.jqVersion = jqVersion;
	}

	public JsonProvider<JsonNode> getJsonProvider() {
		return jsonProvider;
	}

	public Version getJqVersion() {
		return jqVersion;
	}

	public EnvironmentBuilder<JsonNode> setModuleLoader(ModuleLoader<JsonNode> moduleLoader) {
		this.moduleLoader = moduleLoader;
		return this;
	}

	public EnvironmentBuilder<JsonNode> setFunctionLoader(FunctionLoader functionLoader) {
		this.functionLoader = new CachedFunctionLoader(Objects.requireNonNull(functionLoader, "functionLoader"));
		return this;
	}

	/**
	 * Declares {@code name} as a valid global variable with no value -- every query that references it
	 * must be supplied a value via {@link JsonQueryBindings} on every {@code apply()} call, or that call
	 * fails immediately.
	 */
	public EnvironmentBuilder<JsonNode> declareVariable(String name) {
		requireUnusedVariableName(name);
		declaredVariables.add(name);
		return this;
	}

	/**
	 * Declares {@code signature} as a valid global function with no implementation -- every query that
	 * calls it must be supplied an implementation via {@link JsonQueryBindings} on every {@code apply()}
	 * call, or that call fails immediately.
	 */
	public EnvironmentBuilder<JsonNode> declareFunction(FunctionSignature signature) {
		requireUnusedFunctionSignature(signature);
		declaredFunctions.add(signature);
		return this;
	}

	/**
	 * Defines {@code name} with a fixed {@code supplier}, evaluated on every reference. This value is
	 * baked into the compiled query and can never be overridden by {@link JsonQueryBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineVariable(String name, Supplier<JsonNode> supplier) {
		requireUnusedVariableName(name);
		variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(supplier, "supplier"));
		return this;
	}

	/**
	 * Defines {@code name} with a fixed {@code value}. Like {@link #defineVariable}, this can never be
	 * overridden by {@link JsonQueryBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineConstant(String name, JsonNode value) {
		requireUnusedVariableName(name);
		constants.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
		return this;
	}

	/**
	 * Defines {@code nameAndArity} with a fixed {@code function}. Like {@link #defineVariable}, this can
	 * never be overridden by {@link JsonQueryBindings}.
	 */
	public EnvironmentBuilder<JsonNode> defineFunction(FunctionSignature nameAndArity, Function function) {
		requireUnusedFunctionSignature(nameAndArity);
		functions.put(nameAndArity, function);
		return this;
	}

	/**
	 * Defines a jq function whose exact signature is derived from its name and parameter count. The
	 * function is compiled against the completed environment and can never be overridden by
	 * {@link JsonQueryBindings}.
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

	public EnvironmentBuilder<JsonNode> addImportedModule(String name, Module module) {
		importedModules.put(name, module);
		return this;
	}

	public Environment<JsonNode> build() {
		return new EnvironmentImpl<>(jsonProvider, jqVersion, moduleLoader, functionLoader,
				declaredVariables, declaredFunctions, variables, functions, jqFunctions, constants, importedModules);
	}
}
