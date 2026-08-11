package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.MinusOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexMinusAssignment extends ComplexAssignment {
	public ComplexMinusAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MinusOperator());
	}
}
