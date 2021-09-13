package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MinusOperator;

public class MinusExpression extends SimpleBinaryOperatorExpression {
	public MinusExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MinusOperator());
	}
}
