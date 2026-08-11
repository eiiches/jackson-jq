package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.ModuloOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexModuloAssignment extends ComplexAssignment {
	public ComplexModuloAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new ModuloOperator());
	}
}
