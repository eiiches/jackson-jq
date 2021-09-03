package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.LessEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessEqualTest extends SimpleBinaryOperatorExpression {

	public CompareLessEqualTest(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new LessEqualOperator());
	}

	public CompareLessEqualTest() {}
}
