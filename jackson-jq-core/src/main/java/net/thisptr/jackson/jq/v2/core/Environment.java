package net.thisptr.jackson.jq.v2.core;

import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
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

/**
 * Read-only view of a compilation environment. Build one with {@link EnvironmentBuilder}.
 */
public interface Environment<JsonNode> {
	JsonProvider<JsonNode> getJsonProvider();

	Version getJqVersion();

	ModuleLoader<JsonNode> getModuleLoader();

	FunctionLoader getFunctionLoader();

	Map<String, Supplier<JsonNode>> getVariables();

	Map<FunctionSignature, Function> getFunctions();

	Map<String, Module> getImportedModules();

	default JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		return compile(expression, null);
	}

	default JsonQuery<JsonNode> compile(String expression, @Nullable Module currentModule) throws JsonQueryException {
		AstNode parsedAst = AstParser.parse(expression, getJqVersion());
		Expression<JsonNode> compiledExpr = Compiler.compile(this, currentModule, parsedAst);
		if (!(compiledExpr instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		RootExpression<JsonNode> rootExpr = (RootExpression<JsonNode>) compiledExpr;
		return rootExpr::apply;
	}

	default Module compileModule(String source) throws JsonQueryException {
		AstNode parsedAst = AstParser.parse(source + " null", getJqVersion());
		SimpleModule module = new SimpleModule();
		Expression<JsonNode> compiled = Compiler.compileModule(this, module, parsedAst);
		if (!(compiled instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		Map<FunctionSignature, Function> exportedFunctions = ((RootExpression<JsonNode>) compiled).applyForModuleExports(getJsonProvider().createNull());
		exportedFunctions.forEach((key, factory) -> {
			if (key.arity() != null)
				module.addFunction(key, factory);
		});
		module.setModuleMeta(SimpleModuleMeta.fromAst(parsedAst));
		return module;
	}
}
