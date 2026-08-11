package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.operators.LessOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.SimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareLessTest<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public CompareLessTest(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new LessOperator<>());
	}
}
