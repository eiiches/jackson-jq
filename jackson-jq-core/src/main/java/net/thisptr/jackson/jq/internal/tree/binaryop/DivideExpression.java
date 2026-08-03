package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.DivideOperator;

public class DivideExpression extends SimpleBinaryOperatorExpression {
	public DivideExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new DivideOperator());
	}
}
