package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MultiplyOperator;

public class MultiplyExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public MultiplyExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new MultiplyOperator<>());
	}
}
