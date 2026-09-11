package net.thisptr.jackson.jq.v2.core;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.compile.RootExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModule;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModuleMeta;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Read-only view of a compilation environment. Build one with {@link EnvironmentBuilder}.
 */
public interface Environment<JsonNode> {
	JsonProvider<JsonNode> getJsonProvider();

	Version getJqVersion();

	ModuleLoader<JsonNode> getModuleLoader();

	FunctionLoader getFunctionLoader();

	Set<String> getDeclaredVariables();

	Set<FunctionSignature> getDeclaredFunctions();

	Map<String, Supplier<JsonNode>> getVariables();

	Map<FunctionSignature, Function> getFunctions();

	Map<FunctionSignature, JqFunction> getJqFunctions();

	Map<String, JsonNode> getConstants();

	Map<String, Module> getImportedModules();

	/**
	 * Compiles {@code expression} with default options.
	 *
	 * @param expression the jq expression to compile
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled
	 */
	default JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		return compile(expression, new CompileOptions(), null);
	}

	/**
	 * Compiles {@code expression}.
	 *
	 * @param expression the jq expression to compile
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled
	 */
	default JsonQuery<JsonNode> compile(String expression, CompileOptions options) throws JsonQueryException {
		return compile(expression, options, null);
	}

	/**
	 * Compiles {@code expression} as if it were written inside {@code currentModule}, so that the
	 * names that module imported resolve.
	 *
	 * @param expression the jq expression to compile
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @param currentModule the module the expression belongs to, or {@code null} for a bare query
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled
	 */
	default JsonQuery<JsonNode> compile(String expression, CompileOptions options, @Nullable Module currentModule) throws JsonQueryException {
		CompileOptions effectiveOptions = options.copy();
		AstNode parsedAst = AstParser.parse(expression, getJqVersion());
		Expression<StackFrame, JsonNode> compiledExpr = Compiler.compile(this, effectiveOptions, currentModule, parsedAst);
		if (!(compiledExpr instanceof RootExpression))
			throw new IllegalStateException("Compiler did not produce a root expression");
		RootExpression<JsonNode> rootExpr = (RootExpression<JsonNode>) compiledExpr;
		return rootExpr::apply;
	}

	/**
	 * Compiles a module's own source with default options.
	 *
	 * @param source the module source
	 * @return the compiled module
	 * @throws JsonQueryException if {@code source} cannot be parsed or compiled
	 */
	default Module compileModule(String source) throws JsonQueryException {
		return compileModule(source, new CompileOptions());
	}

	/**
	 * Compiles a module's own source.
	 *
	 * @param source the module source
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @return the compiled module
	 * @throws JsonQueryException if {@code source} cannot be parsed or compiled
	 */
	default Module compileModule(String source, CompileOptions options) throws JsonQueryException {
		CompileOptions effectiveOptions = options.copy();
		AstNode parsedAst = AstParser.parse(source + " null", getJqVersion());
		SimpleModule module = new SimpleModule();
		Expression<StackFrame, JsonNode> compiled = Compiler.compileModule(this, effectiveOptions, module, parsedAst);
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
