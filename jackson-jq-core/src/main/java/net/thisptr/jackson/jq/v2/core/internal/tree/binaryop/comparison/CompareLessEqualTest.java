package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class CompareLessEqualTest<JsonNode> extends AbstractComparisonExpression<JsonNode> {
	public CompareLessEqualTest(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs) {
		super(jsonProvider, lhs, rhs);
	}

	@Override
	protected boolean test(int r) {
		return r <= 0;
	}
}
