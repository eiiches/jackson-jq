package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MinusOperator;

public class MinusExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public MinusExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new MinusOperator<>());
	}
}
