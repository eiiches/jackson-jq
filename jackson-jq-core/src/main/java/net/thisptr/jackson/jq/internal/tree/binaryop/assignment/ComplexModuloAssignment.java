package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.ModuloOperator;

public class ComplexModuloAssignment extends ComplexAssignment {
	public ComplexModuloAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new ModuloOperator());
	}
}
