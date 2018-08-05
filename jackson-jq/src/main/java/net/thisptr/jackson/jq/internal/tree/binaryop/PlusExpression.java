package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;

public class PlusExpression extends SimpleBinaryOperatorExpression {
	public PlusExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new PlusOperator());
	}
}
