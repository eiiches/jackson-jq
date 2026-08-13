package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.GreaterEqualOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareGreaterEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareGreaterEqualTest(Expression lhs, Expression rhs) {
		super(lhs, rhs, new GreaterEqualOperator<>());
	}
}
