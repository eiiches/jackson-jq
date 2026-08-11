package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexPlusAssignment extends ComplexAssignment {
	public ComplexPlusAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new PlusOperator());
	}
}
