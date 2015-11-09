package net.thisptr.jackson.jq.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.LessOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.SimpleBinaryOperatorExpression;

public class CompareLessTest extends SimpleBinaryOperatorExpression {
	public CompareLessTest(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new LessOperator());
	}
}
