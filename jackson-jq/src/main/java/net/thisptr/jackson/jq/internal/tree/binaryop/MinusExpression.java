package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.MinusOperator;

public class MinusExpression extends SimpleBinaryOperatorExpression {
	public MinusExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new MinusOperator());
	}
}
