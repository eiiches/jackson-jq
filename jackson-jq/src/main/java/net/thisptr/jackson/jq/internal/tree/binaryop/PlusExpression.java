package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;

public class PlusExpression extends SimpleBinaryOperatorExpression {
	public PlusExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new PlusOperator());
	}
}
