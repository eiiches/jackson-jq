package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ComplexMinusAssignment<JsonNode> extends AbstractComplexAssignment<JsonNode> {
	public ComplexMinusAssignment(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, Version version, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs) {
		return new ComplexMinusAssignment<>(jsonProvider, rewrittenLhs, rewrittenRhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected JsonNode eval(RuntimeLimits limits, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return BinaryOperations.minus(jsonProvider, lhs, rhs, version);
	}
}
