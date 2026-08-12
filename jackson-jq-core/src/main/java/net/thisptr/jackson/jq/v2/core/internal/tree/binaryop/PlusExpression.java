package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class PlusExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public PlusExpression(Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, new PlusOperator<>());
	}
}
