package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.operators.ModuloOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexModuloAssignment<JsonNode> extends ComplexAssignment<JsonNode> {
	public ComplexModuloAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, boolean inputFixed) {
		super(jsonProvider, lhs, rhs, new ModuloOperator<>(), inputFixed);
	}
}
