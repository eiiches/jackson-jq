package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.GreaterOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareGreaterTest extends SimpleBinaryOperatorExpression {
	public CompareGreaterTest(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new GreaterOperator());
	}
}
