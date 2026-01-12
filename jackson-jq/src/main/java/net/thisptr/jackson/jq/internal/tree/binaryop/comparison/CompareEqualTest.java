package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.EqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareEqualTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new EqualOperator<>());
	}
}
