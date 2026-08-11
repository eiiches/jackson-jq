package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.GreaterOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareGreaterTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareGreaterTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new GreaterOperator<>());
	}
}
