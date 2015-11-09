package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.OrOperator;

public class BooleanOrExpression extends SimpleBinaryOperatorExpression {
	public BooleanOrExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new OrOperator());
	}
}
