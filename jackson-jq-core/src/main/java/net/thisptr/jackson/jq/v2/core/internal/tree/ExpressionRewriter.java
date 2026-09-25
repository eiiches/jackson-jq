package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;

/**
 * Rewrites one expression subtree.
 *
 * @param <JsonNode> the JSON node type
 */
@FunctionalInterface
public interface ExpressionRewriter<JsonNode> {
	AnalyzedExpression<JsonNode> rewrite(AnalyzedExpression<JsonNode> expression);

	static <N> List<AnalyzedExpression<N>> rewriteAll(List<AnalyzedExpression<N>> expressions, ExpressionRewriter<N> rewriter) {
		@Var List<AnalyzedExpression<N>> rewritten = null;
		for (int i = 0; i < expressions.size(); i++) {
			AnalyzedExpression<N> expression = expressions.get(i);
			AnalyzedExpression<N> replacement = rewriter.rewrite(expression);
			if (rewritten == null && replacement != expression)
				rewritten = new ArrayList<>(expressions);
			if (rewritten != null)
				rewritten.set(i, replacement);
		}
		return rewritten != null ? rewritten : expressions;
	}
}
