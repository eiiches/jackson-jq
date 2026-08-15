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
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class Environment<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private @Nullable ModuleLoader<JsonNode> moduleLoader;
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionNameAndArity, FunctionFactory> functionFactories = new HashMap<>();

	public Environment(JsonProvider<JsonNode> jsonProvider, Version version) {
		this.jsonProvider = jsonProvider;
		this.version = version;
	}

	public JsonProvider<JsonNode> jsonProvider() {
		return jsonProvider;
	}

	public Version version() {
		return version;
	}

	public Environment<JsonNode> setModuleLoader(ModuleLoader<JsonNode> moduleLoader) {
		this.moduleLoader = moduleLoader;
		return this;
	}

	public @Nullable ModuleLoader<JsonNode> getModuleLoader() {
		return moduleLoader;
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

	public @Nullable FunctionFactory getFunctionFactory(FunctionNameAndArity nameAndArity) {
		FunctionFactory factory = functionFactories.get(nameAndArity);
		if (factory != null)
			return factory;
		return functionFactories.get(nameAndArity.withArity(null));
	}

	public CompiledQuery<JsonNode> compile(String expression) throws JsonQueryException {
		Expression parsedExpr = ExpressionParser.compile(expression, version);
		Expression resolvedExpr = AstResolver.resolve(this, parsedExpr);
		return (in, output) -> resolvedExpr.apply(Scope.newEmptyScope(jsonProvider), in, null, output, false);
	}
}
