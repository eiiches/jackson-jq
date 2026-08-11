package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.MultiplyOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexMultiplyAssignment extends ComplexAssignment {
	public ComplexMultiplyAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MultiplyOperator());
	}
}
