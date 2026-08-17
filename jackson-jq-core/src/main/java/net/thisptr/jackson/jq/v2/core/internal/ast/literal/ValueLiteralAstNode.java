package net.thisptr.jackson.jq.v2.core.internal.ast.literal;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public abstract class ValueLiteralAstNode implements AstNode {

	/**
	 * Pure compile-time value computation (no {@code Frame}/execution involved) -- used by
	 * {@link ExpressionUtils#evaluateLiteralExpression}
	 * to evaluate constant module metadata before compilation ever runs.
	 */
	public abstract <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider);
}
