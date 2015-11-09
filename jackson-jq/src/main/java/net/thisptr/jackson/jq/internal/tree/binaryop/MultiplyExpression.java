package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.MultiplyOperator;

public class MultiplyExpression extends SimpleBinaryOperatorExpression {
	public MultiplyExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new MultiplyOperator());
	}
}
