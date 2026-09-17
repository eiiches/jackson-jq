package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ComplexAlternativeAssignment<JsonNode> extends AbstractComplexAssignment<JsonNode> {
	public ComplexAlternativeAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected Expression<StackFrame, JsonNode> recreate(Expression<StackFrame, JsonNode> rewrittenLhs, Expression<StackFrame, JsonNode> rewrittenRhs) {
		return new ComplexAlternativeAssignment<>(jsonProvider, rewrittenLhs, rewrittenRhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected JsonNode eval(RuntimeLimits limits, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return BinaryOperations.alternative(jsonProvider, lhs, rhs);
	}
}
