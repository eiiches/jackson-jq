package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.LessEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessEqualTest extends SimpleBinaryOperatorExpression {
	public CompareLessEqualTest(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new LessEqualOperator());
	}
}
