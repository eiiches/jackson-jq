package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.DivideOperator;

public class DivideExpression extends SimpleBinaryOperatorExpression {
	public DivideExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new DivideOperator());
	}
}
