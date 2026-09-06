package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public abstract class AbstractValueLiteralAstNode implements AstNode {

	/**
	 * Pure compile-time value computation (no {@code Frame}/execution involved) -- used by
	 * {@link net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils#evaluateLiteralExpression}
	 * to evaluate constant module metadata before compilation ever runs.
	 */
	public abstract <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider);
}
