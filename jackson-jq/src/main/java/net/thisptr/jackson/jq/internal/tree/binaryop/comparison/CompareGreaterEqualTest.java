package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.GreaterEqualOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareGreaterEqualTest extends SimpleBinaryOperatorExpression {
	public CompareGreaterEqualTest(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new GreaterEqualOperator());
	}
}
