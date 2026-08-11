package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class PlusExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public PlusExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new PlusOperator<>());
	}
}
