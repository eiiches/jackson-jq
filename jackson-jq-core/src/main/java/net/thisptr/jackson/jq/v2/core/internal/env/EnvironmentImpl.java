package net.thisptr.jackson.jq.v2.core.internal.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.compile.QueryCompiler;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Concrete {@link Environment}. Build one with {@code EnvironmentBuilder}, not directly.
 */
public class EnvironmentImpl<JsonNode> implements Environment<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version jqVersion;
	private final List<ModuleLoader<JsonNode>> moduleLoaders;
	private final List<FunctionLoader> functionLoaders;
	private final Set<String> declaredVariables;
	private final Set<FunctionSignature> declaredFunctions;
	private final Map<String, Supplier<JsonNode>> variables;
	private final Map<FunctionSignature, Function> functions;
	private final Map<FunctionSignature, JqFunction> jqFunctions;
	private final Map<String, JsonNode> constants;
	private final Map<String, Module> importedModules;

	public EnvironmentImpl(JsonProvider<JsonNode> jsonProvider, Version jqVersion,
						   List<ModuleLoader<JsonNode>> moduleLoaders, List<FunctionLoader> functionLoaders,
						   Set<String> declaredVariables, Set<FunctionSignature> declaredFunctions,
						   Map<String, Supplier<JsonNode>> variables, Map<FunctionSignature, Function> functions,
						   Map<FunctionSignature, JqFunction> jqFunctions,
						   Map<String, JsonNode> constants, Map<String, Module> importedModules) {
		this.jsonProvider = jsonProvider;
		this.jqVersion = jqVersion;
		this.moduleLoaders = new ArrayList<>(moduleLoaders);
		this.functionLoaders = new ArrayList<>(functionLoaders);
		this.declaredVariables = new HashSet<>(declaredVariables);
		this.declaredFunctions = new HashSet<>(declaredFunctions);
		this.variables = new HashMap<>(variables);
		this.functions = new HashMap<>(functions);
		this.jqFunctions = new HashMap<>(jqFunctions);
		this.constants = new HashMap<>(constants);
		this.importedModules = new HashMap<>(importedModules);
	}

	@Override
	public JsonProvider<JsonNode> getJsonProvider() {
		return jsonProvider;
	}

	@Override
	public Version getJqVersion() {
		return jqVersion;
	}

	@Override
	public List<ModuleLoader<JsonNode>> getModuleLoaders() {
		return Collections.unmodifiableList(moduleLoaders);
	}

	@Override
	public List<FunctionLoader> getFunctionLoaders() {
		return Collections.unmodifiableList(functionLoaders);
	}

	@Override
	public Set<String> getDeclaredVariables() {
		return Collections.unmodifiableSet(declaredVariables);
	}

	@Override
	public Set<FunctionSignature> getDeclaredFunctions() {
		return Collections.unmodifiableSet(declaredFunctions);
	}

	@Override
	public Map<String, Supplier<JsonNode>> getVariables() {
		return Collections.unmodifiableMap(variables);
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}

	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions() {
		return Collections.unmodifiableMap(jqFunctions);
	}

	@Override
	public Map<String, JsonNode> getConstants() {
		return Collections.unmodifiableMap(constants);
	}

	@Override
	public Map<String, Module> getImportedModules() {
		return Collections.unmodifiableMap(importedModules);
	}

	@Override
	public JsonQuery<JsonNode> compile(String expression, CompileOptions options) throws JsonQueryException {
		return QueryCompiler.compile(this, expression, options);
	}
}
