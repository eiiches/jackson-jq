package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.DivideOperator;

public class DivideExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public DivideExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new DivideOperator<>());
	}
}
