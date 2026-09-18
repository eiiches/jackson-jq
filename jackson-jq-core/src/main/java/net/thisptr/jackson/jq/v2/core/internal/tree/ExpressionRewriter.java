package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * Rewrites one expression subtree.
 *
 * @param <JsonNode> the JSON node type
 */
@FunctionalInterface
public interface ExpressionRewriter<JsonNode> {
	Expression<StackFrame, JsonNode> rewrite(Expression<StackFrame, JsonNode> expression);

	static <N> List<Expression<StackFrame, N>> rewriteAll(List<Expression<StackFrame, N>> expressions, ExpressionRewriter<N> rewriter) {
		@Var List<Expression<StackFrame, N>> rewritten = null;
		for (int i = 0; i < expressions.size(); i++) {
			Expression<StackFrame, N> expression = expressions.get(i);
			Expression<StackFrame, N> replacement = rewriter.rewrite(expression);
			if (rewritten == null && replacement != expression)
				rewritten = new ArrayList<>(expressions);
			if (rewritten != null)
				rewritten.set(i, replacement);
		}
		return rewritten != null ? rewritten : expressions;
	}
}
