package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.GreaterOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareGreaterTest extends SimpleBinaryOperatorExpression {
	public CompareGreaterTest(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new GreaterOperator());
	}

	public CompareGreaterTest() {}
}
