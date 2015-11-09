package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.MultiplyOperator;

public class ComplexMultiplyAssignment extends ComplexAssignment {
	public ComplexMultiplyAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new MultiplyOperator());
	}
}
