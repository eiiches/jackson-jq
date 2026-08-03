package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.MultiplyOperator;

public class ComplexMultiplyAssignment extends ComplexAssignment {
	public ComplexMultiplyAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new MultiplyOperator());
	}
}
