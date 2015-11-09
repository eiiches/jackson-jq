package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;

public class ComplexPlusAssignment extends ComplexAssignment {
	public ComplexPlusAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new PlusOperator());
	}
}
