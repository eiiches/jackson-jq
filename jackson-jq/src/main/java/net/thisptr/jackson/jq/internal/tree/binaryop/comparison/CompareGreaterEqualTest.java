package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.GreaterEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareGreaterEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareGreaterEqualTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new GreaterEqualOperator<>());
	}
}
