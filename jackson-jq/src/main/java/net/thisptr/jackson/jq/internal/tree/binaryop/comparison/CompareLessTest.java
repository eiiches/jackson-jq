package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.LessOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareLessTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new LessOperator<>());
	}
}
