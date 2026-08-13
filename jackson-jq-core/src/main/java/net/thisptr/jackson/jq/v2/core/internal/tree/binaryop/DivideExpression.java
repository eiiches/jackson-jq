package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.DivideOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class DivideExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public DivideExpression(Expression lhs, Expression rhs) {
		super(lhs, rhs, new DivideOperator<>());
	}
}
