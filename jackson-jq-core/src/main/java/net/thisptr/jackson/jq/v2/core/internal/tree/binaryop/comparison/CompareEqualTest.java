package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.EqualOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareEqualTest(Expression lhs, Expression rhs) {
		super(lhs, rhs, new EqualOperator<>());
	}
}
