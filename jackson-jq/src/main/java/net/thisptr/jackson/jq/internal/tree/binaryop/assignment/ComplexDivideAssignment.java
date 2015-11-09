package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.DivideOperator;

public class ComplexDivideAssignment extends ComplexAssignment {
	public ComplexDivideAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new DivideOperator());
	}
}
