package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.ModuloOperator;

public class ModuloExpression extends SimpleBinaryOperatorExpression {
	public ModuloExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, new ModuloOperator());
	}
}
