package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.NotEqualOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareNotEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareNotEqualTest(Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, new NotEqualOperator<>());
	}
}
