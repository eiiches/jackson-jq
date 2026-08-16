package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.AstResolver;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class Environment<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private @Nullable ModuleLoader<JsonNode> moduleLoader;
	private @Nullable FunctionLoader functionLoader;
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionNameAndArity, FunctionFactory> functionFactories = new HashMap<>();
	private final Scope<JsonNode> rootScope;

	public Environment(JsonProvider<JsonNode> jsonProvider, Version version) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.rootScope = Scope.newEmptyScope(jsonProvider);
		this.functionLoader = BuiltinFunctionLoader.getInstance();
		Map<FunctionNameAndArity, FunctionFactory> builtins = this.functionLoader.listFunctionFactories(version);
		this.functionFactories.putAll(builtins);
	}

	public JsonProvider<JsonNode> jsonProvider() {
		return jsonProvider;
	}

	public Version version() {
		return version;
	}

	public Scope<JsonNode> rootScope() {
		return rootScope;
	}

	public Environment<JsonNode> setModuleLoader(ModuleLoader<JsonNode> moduleLoader) {
		this.moduleLoader = moduleLoader;
		this.rootScope.setModuleLoader(moduleLoader);
		return this;
	}

	public @Nullable ModuleLoader<JsonNode> getModuleLoader() {
		return moduleLoader;
	}

	public Environment<JsonNode> setFunctionLoader(FunctionLoader functionLoader) {
		this.functionLoader = functionLoader;
		Map<FunctionNameAndArity, FunctionFactory> factories = functionLoader.listFunctionFactories(version);
		this.functionFactories.putAll(factories);
		return this;
	}

	public @Nullable FunctionLoader getFunctionLoader() {
		return functionLoader;
	}

	public Environment<JsonNode> addVariable(String name, Supplier<JsonNode> supplier) {
		variables.put(name, supplier);
		return this;
	}

	public Environment<JsonNode> addVariable(String name, JsonNode value) {
		return addVariable(name, () -> value);
	}

	public @Nullable Supplier<JsonNode> getVariable(String name) {
		return variables.get(name);
	}

	public Environment<JsonNode> addFunctionFactory(FunctionNameAndArity nameAndArity, FunctionFactory functionFactory) {
		functionFactories.put(nameAndArity, functionFactory);
		return this;
	}

	public Map<FunctionNameAndArity, FunctionFactory> functionFactories() {
		return java.util.Collections.unmodifiableMap(functionFactories);
	}

	public @Nullable FunctionFactory getFunctionFactory(FunctionNameAndArity nameAndArity) {
		FunctionFactory factory = functionFactories.get(nameAndArity);
		if (factory != null)
			return factory;
		return functionFactories.get(nameAndArity.withArity(null));
	}

	public JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		Expression parsedExpr = ExpressionParser.compile(expression, version);
		Expression resolvedExpr = AstResolver.resolve(this, parsedExpr);
		return (in, output) -> resolvedExpr.apply(rootScope, in, null, output, false);
	}
}
