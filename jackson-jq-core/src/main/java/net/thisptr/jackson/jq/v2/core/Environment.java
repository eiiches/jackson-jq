package net.thisptr.jackson.jq.v2.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModule;
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
public class Environment<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private @Nullable ModuleLoader<JsonNode> moduleLoader;
	private @Nullable FunctionLoader functionLoader;
	private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public Environment(JsonProvider<JsonNode> jsonProvider, Version version) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.functionLoader = ClassPathFunctionLoader.getInstance();
		Map<FunctionSignature, Function> builtins = this.functionLoader.listFunctions(version);
		this.functions.putAll(builtins);
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
		Map<FunctionSignature, Function> factories = functionLoader.listFunctions(version);
		this.functions.putAll(factories);
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
		return Collections.unmodifiableMap(variables);
	}

	public Environment<JsonNode> addFunction(FunctionSignature nameAndArity, Function function) {
		functions.put(nameAndArity, function);
		return this;
	}

	public Map<FunctionSignature, Function> functions() {
		return Collections.unmodifiableMap(functions);
	}

	public @Nullable Function resolveFunction(String fname, int nargs) {
		Function factory = functions.get(FunctionSignature.of(fname, nargs));
		if (factory != null)
			return factory;
		return functions.get(FunctionSignature.of(fname, nargs).withArity(null));
	}

	public JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		return compile(expression, null);
	}

	public JsonQuery<JsonNode> compile(String expression, @Nullable Module currentModule) throws JsonQueryException {
		AstNode parsedAst = AstParser.parse(expression, version);
		Expression<JsonNode> compiledExpr = Compiler.compile(this, currentModule, parsedAst);
		if (!(compiledExpr instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		RootExpression<JsonNode> rootExpr = (RootExpression<JsonNode>) compiledExpr;
		return rootExpr::apply;
	}

	public Module compileModule(String source) throws JsonQueryException {
		AstNode parsedAst = AstParser.parse(source + " null", version);
		SimpleModule module = new SimpleModule();
		Expression<JsonNode> compiled = Compiler.compileModule(this, module, parsedAst);
		if (!(compiled instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		Map<FunctionSignature, Function> exportedFunctions = ((RootExpression<JsonNode>) compiled).applyForModuleExports(jsonProvider.createNull());
		exportedFunctions.forEach((key, factory) -> {
			if (key.arity() != null)
				module.addFunction(key, factory);
		});
		return module;
	}
}
