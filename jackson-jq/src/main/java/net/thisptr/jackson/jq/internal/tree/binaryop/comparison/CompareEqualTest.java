package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.EqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareEqualTest extends SimpleBinaryOperatorExpression {
	public CompareEqualTest(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new EqualOperator());
	}
}
