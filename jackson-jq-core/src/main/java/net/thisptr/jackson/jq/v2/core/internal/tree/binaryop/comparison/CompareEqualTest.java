package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareEqualTest<JsonNode> extends AbstractComparisonExpression<JsonNode> {
	public CompareEqualTest(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected Expression<StackFrame, JsonNode> recreate(Expression<StackFrame, JsonNode> rewrittenLhs, Expression<StackFrame, JsonNode> rewrittenRhs) {
		return new CompareEqualTest<>(jsonProvider, rewrittenLhs, rewrittenRhs, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	protected boolean test(int r) {
		return r == 0;
	}
}
