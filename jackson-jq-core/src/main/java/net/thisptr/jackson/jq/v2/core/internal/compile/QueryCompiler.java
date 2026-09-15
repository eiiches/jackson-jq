package net.thisptr.jackson.jq.v2.core.internal.compile;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Compiles user-facing jq queries and adapts the compiler's root expression to {@link JsonQuery}.
 */
public final class QueryCompiler {
	private QueryCompiler() {
	}

	public static <JsonNode> JsonQuery<JsonNode> compile(Environment<JsonNode> env, String expression, CompileOptions options) throws JsonQueryException {
		try {
			AstNode parsedAst = AstParser.parse(expression, env.getJqVersion());
			Expression<StackFrame, JsonNode> compiledExpr = Compiler.compile(env, options, ModuleScope.root(env), parsedAst);
			if (!(compiledExpr instanceof RootExpression))
				throw new IllegalStateException("Compiler did not produce a root expression");
			RootExpression<JsonNode> rootExpr = (RootExpression<JsonNode>) compiledExpr;
			return new CompiledJsonQuery<>(rootExpr, RuntimeLimitsImpl.UNLIMITED);
		} catch (JsonQueryException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new JsonQueryException("Stack overflow during compilation", e);
		} catch (RuntimeException e) {
			throw new JsonQueryException("Unexpected exception during compilation", e);
		}
	}
}
