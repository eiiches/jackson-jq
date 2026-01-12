package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.NotEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareNotEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareNotEqualTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new NotEqualOperator<>());
	}
}
