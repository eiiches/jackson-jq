package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.Objects;

import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class MultiplyExpression<JsonNode> extends AbstractSimpleBinaryOperatorExpression<JsonNode> {
	private final Version version;

	public MultiplyExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version) {
		super(jsonProvider, lhs, rhs, "*");
		this.version = Objects.requireNonNull(version, "version");
	}

	@Override
	protected JsonNode doEval(JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return BinaryOperations.multiply(jsonProvider, lhs, rhs, version);
	}
}
