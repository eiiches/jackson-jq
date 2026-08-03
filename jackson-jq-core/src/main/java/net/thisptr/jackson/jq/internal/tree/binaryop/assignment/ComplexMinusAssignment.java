package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MinusOperator;

public class ComplexMinusAssignment extends ComplexAssignment {
	public ComplexMinusAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MinusOperator());
	}
}
