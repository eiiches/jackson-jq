package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;

public class ComplexPlusAssignment extends ComplexAssignment {
	public ComplexPlusAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new PlusOperator());
	}
}
