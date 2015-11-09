package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.NotEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareNotEqualTest extends SimpleBinaryOperatorExpression {
	public CompareNotEqualTest(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new NotEqualOperator());
	}
}
