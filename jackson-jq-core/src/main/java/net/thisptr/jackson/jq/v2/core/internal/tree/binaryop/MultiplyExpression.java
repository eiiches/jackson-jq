package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.Objects;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class MultiplyExpression<JsonNode> extends AbstractSimpleBinaryOperatorExpression<JsonNode> {
	private final Version version;

	public MultiplyExpression(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, Version version, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
		this.version = Objects.requireNonNull(version, "version");
	}

	@Override
	protected AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs) {
		return new MultiplyExpression<>(jsonProvider, rewrittenLhs, rewrittenRhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected JsonNode doEval(RuntimeLimits limits, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return BinaryOperations.multiply(jsonProvider, limits, lhs, rhs, version);
	}
}
