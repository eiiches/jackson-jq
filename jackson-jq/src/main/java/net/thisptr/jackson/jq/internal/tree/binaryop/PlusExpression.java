package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;

public class PlusExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public PlusExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new PlusOperator<>());
	}
}
