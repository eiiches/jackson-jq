package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.MultiplyOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class MultiplyExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public MultiplyExpression(Expression lhs, Expression rhs) {
		super(lhs, rhs, new MultiplyOperator<>());
	}
}
