package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.AndOperator;

public class BooleanAndExpression extends SimpleBinaryOperatorExpression {
	public BooleanAndExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new AndOperator());
	}
}
