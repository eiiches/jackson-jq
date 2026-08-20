package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.internal.env.EnvironmentImpl;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Fluent builder for {@link Environment}. {@code Environment} itself is a read-only view; all
 * configuration happens here, before {@link #build()}.
 */
public final class EnvironmentBuilder<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version jqVersion;

	private ModuleLoader<JsonNode> moduleLoader = new ClassPathModuleLoader<>(EnvironmentBuilder.class.getClassLoader());
	private FunctionLoader functionLoader = new CachedFunctionLoader(new ClassPathFunctionLoader(EnvironmentBuilder.class.getClassLoader()));

	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
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

	public EnvironmentBuilder<JsonNode> addVariable(String name, Supplier<JsonNode> supplier) {
		variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(supplier, "supplier"));
		return this;
	}

	public EnvironmentBuilder<JsonNode> addVariable(String name, JsonNode value) {
		return addVariable(name, () -> value);
	}

	public EnvironmentBuilder<JsonNode> addFunction(FunctionSignature nameAndArity, Function function) {
		functions.put(nameAndArity, function);
		return this;
	}

	public EnvironmentBuilder<JsonNode> addImportedModule(String name, Module module) {
		importedModules.put(name, module);
		return this;
	}

	public Environment<JsonNode> build() {
		return new EnvironmentImpl<>(jsonProvider, jqVersion, moduleLoader, functionLoader, variables, functions, importedModules);
	}
}
