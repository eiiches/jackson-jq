package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.LessOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessTest extends SimpleBinaryOperatorExpression {
	public CompareLessTest(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new LessOperator());
	}

	public CompareLessTest() {}
}
