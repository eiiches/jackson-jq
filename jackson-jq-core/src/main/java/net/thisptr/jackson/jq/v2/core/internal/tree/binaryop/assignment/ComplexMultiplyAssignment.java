package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ComplexMultiplyAssignment<JsonNode> extends AbstractComplexAssignment<JsonNode> {
	public ComplexMultiplyAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, boolean inputFixed) {
		super(jsonProvider, lhs, rhs, version, inputFixed);
	}

	@Override
	protected JsonNode eval(JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return BinaryOperations.multiply(jsonProvider, lhs, rhs, version);
	}
}
