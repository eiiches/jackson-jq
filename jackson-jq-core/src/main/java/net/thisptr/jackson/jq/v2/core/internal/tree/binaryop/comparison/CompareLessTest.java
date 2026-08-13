package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.LessOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareLessTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareLessTest(Expression lhs, Expression rhs) {
		super(lhs, rhs, new LessOperator<>());
	}
}
