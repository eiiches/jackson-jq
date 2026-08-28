package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.operators.MultiplyOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;

public class ComplexMultiplyAssignment<JsonNode> extends ComplexAssignment<JsonNode> {
	public ComplexMultiplyAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, boolean inputFixed) {
		super(jsonProvider, lhs, rhs, new MultiplyOperator<>(), version, inputFixed);
	}
}
