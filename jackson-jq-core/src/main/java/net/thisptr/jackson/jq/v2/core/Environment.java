package net.thisptr.jackson.jq.v2.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.compile.ModuleScope;
import net.thisptr.jackson.jq.v2.core.internal.compile.RootExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Read-only view of a compilation environment. Build one with {@link EnvironmentBuilder}.
 */
public interface Environment<JsonNode> {
	JsonProvider<JsonNode> getJsonProvider();

	Version getJqVersion();

	/**
	 * The module loaders this environment consults, in the order they are asked.
	 */
	List<ModuleLoader<JsonNode>> getModuleLoaders();

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
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled; other runtime
	 * exceptions and stack overflows during compilation are wrapped in a {@code JsonQueryException}
	 */
	default JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		return compile(expression, CompileOptions.getDefaultInstance(), null);
	}

	/**
	 * Compiles {@code expression}.
	 *
	 * @param expression the jq expression to compile
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled; other runtime
	 * exceptions and stack overflows during compilation are wrapped in a {@code JsonQueryException}
	 */
	default JsonQuery<JsonNode> compile(String expression, CompileOptions options) throws JsonQueryException {
		return compile(expression, options, null);
	}

	/**
	 * Compiles {@code expression} as if it were written inside {@code currentModule}, so that an
	 * import relative to that module resolves the way it would from inside it.
	 *
	 * @param expression the jq expression to compile
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @param currentModule the module the expression belongs to, or {@code null} for a bare query
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled; other runtime
	 * exceptions and stack overflows during compilation are wrapped in a {@code JsonQueryException}
	 */
	default JsonQuery<JsonNode> compile(String expression, CompileOptions options, @Nullable JqModule currentModule) throws JsonQueryException {
		try {
			AstNode parsedAst = AstParser.parse(expression, getJqVersion());
			Expression<StackFrame, JsonNode> compiledExpr = Compiler.compile(this, options, ModuleScope.<JsonNode>root(this).inside(currentModule), parsedAst);
			if (!(compiledExpr instanceof RootExpression))
				throw new IllegalStateException("Compiler did not produce a root expression");
			RootExpression<JsonNode> rootExpr = (RootExpression<JsonNode>) compiledExpr;
			return (in, runtimeOptions, bindings, output) -> {
				try {
					RuntimeLimits runtimeLimits = new RuntimeLimitsImpl(runtimeOptions.getMaxArrayLength(), runtimeOptions.getMaxObjectMemberCount(), runtimeOptions.getMaxStringLength());
					rootExpr.apply(in, runtimeLimits, bindings, output);
				} catch (JsonQueryException e) {
					throw e;
				} catch (StackOverflowError e) {
					throw new JsonQueryException("Stack overflow during evaluation", e);
				} catch (RuntimeException e) {
					throw new JsonQueryException("Unexpected exception during evaluation", e);
				}
			};
		} catch (JsonQueryException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new JsonQueryException("Stack overflow during compilation", e);
		} catch (RuntimeException e) {
			throw new JsonQueryException("Unexpected exception during compilation", e);
		}
	}
}
