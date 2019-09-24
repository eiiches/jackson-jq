package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.AlternativeOperator;

public class ComplexAlternativeAssignment extends ComplexAssignment {
	public ComplexAlternativeAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new AlternativeOperator());
	}
}
