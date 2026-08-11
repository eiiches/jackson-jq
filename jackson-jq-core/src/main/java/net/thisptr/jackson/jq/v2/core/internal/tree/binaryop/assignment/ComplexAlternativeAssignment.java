package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.AlternativeOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexAlternativeAssignment extends ComplexAssignment {
	public ComplexAlternativeAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new AlternativeOperator());
	}
}
