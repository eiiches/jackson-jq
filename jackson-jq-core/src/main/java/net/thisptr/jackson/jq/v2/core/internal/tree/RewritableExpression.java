package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;

/**
 * An internal expression node that can immutably rewrite its directly owned expression subtrees.
 *
 * @param <JsonNode> the JSON node type
 */
public interface RewritableExpression<JsonNode> extends AnalyzedExpression<JsonNode> {
	/**
	 * Applies {@code rewriter} once to each directly owned expression subtree and returns either this node
	 * when none changed or an equivalent node containing the replacements.
	 *
	 * @param rewriter the subtree rewriter
	 * @return this node or its rewritten replacement
	 */
	AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter);
}
