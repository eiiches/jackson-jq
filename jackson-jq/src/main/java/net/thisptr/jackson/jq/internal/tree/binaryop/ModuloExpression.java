package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.operators.ModuloOperator;

public class ModuloExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public ModuloExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, new ModuloOperator<>());
	}
}
