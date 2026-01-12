package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.LessEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareLessEqualTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new LessEqualOperator<>());
	}
}
