package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.DivideOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexDivideAssignment extends ComplexAssignment {
	public ComplexDivideAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new DivideOperator());
	}
}
