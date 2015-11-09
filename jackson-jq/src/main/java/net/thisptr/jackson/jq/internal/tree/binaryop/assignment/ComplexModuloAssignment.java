package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.internal.operators.ModuloOperator;

public class ComplexModuloAssignment extends ComplexAssignment {
	public ComplexModuloAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, new ModuloOperator());
	}
}
