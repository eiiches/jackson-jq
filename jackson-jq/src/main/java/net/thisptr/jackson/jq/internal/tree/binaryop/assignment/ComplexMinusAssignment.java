package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.MinusOperator;

public class ComplexMinusAssignment extends ComplexAssignment {
	public ComplexMinusAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new MinusOperator());
	}
}
