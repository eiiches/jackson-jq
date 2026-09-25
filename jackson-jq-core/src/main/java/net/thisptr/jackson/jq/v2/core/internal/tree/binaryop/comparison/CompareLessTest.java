package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class CompareLessTest<JsonNode> extends AbstractComparisonExpression<JsonNode> {
	public CompareLessTest(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs) {
		return new CompareLessTest<>(jsonProvider, rewrittenLhs, rewrittenRhs, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected boolean test(int r) {
		return r < 0;
	}
}
