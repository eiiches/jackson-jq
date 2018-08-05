package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MultiplyOperator;

public class MultiplyExpression extends SimpleBinaryOperatorExpression {
	public MultiplyExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MultiplyOperator());
	}
}
