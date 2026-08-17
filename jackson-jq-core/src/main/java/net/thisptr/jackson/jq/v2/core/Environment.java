package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
public class Environment<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private @Nullable ModuleLoader<JsonNode> moduleLoader;
	private @Nullable FunctionLoader functionLoader;
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionNameAndArity, FunctionFactory> functionFactories = new HashMap<>();

	public Environment(JsonProvider<JsonNode> jsonProvider, Version version) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.functionLoader = ClassPathFunctionLoader.getInstance();
		Map<FunctionNameAndArity, FunctionFactory> builtins = this.functionLoader.listFunctionFactories(version);
		this.functionFactories.putAll(builtins);
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
		variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(supplier, "supplier"));
		return this;
	}

	public Environment<JsonNode> addVariable(String name, JsonNode value) {
		return addVariable(name, () -> value);
	}

	public @Nullable Supplier<JsonNode> getVariable(String name) {
		return variables.get(name);
	}

	public Map<String, Supplier<JsonNode>> variables() {
		return java.util.Collections.unmodifiableMap(variables);
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
		return compile(expression, null);
	}

	public JsonQuery<JsonNode> compile(String expression, @Nullable Module currentModule) throws JsonQueryException {
		AstNode parsedAst = AstParser.parse(expression, version);
		Expression<JsonNode> compiledExpr = Compiler.compile(this, currentModule, parsedAst);
		if (!(compiledExpr instanceof net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		@SuppressWarnings("unchecked")
		net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression<JsonNode> rootExpr =
				(net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression<JsonNode>) compiledExpr;
		return rootExpr::apply;
	}
}
