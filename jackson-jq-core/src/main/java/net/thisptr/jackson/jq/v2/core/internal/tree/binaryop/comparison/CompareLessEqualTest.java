package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.LessEqualOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareLessEqualTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareLessEqualTest(Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, new LessEqualOperator<>());
	}
}
