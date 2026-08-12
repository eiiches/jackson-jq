package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.MinusOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class MinusExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public MinusExpression(Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, new MinusOperator<>());
	}
}
