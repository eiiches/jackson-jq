package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison;

import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AbstractSimpleBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public abstract class AbstractComparisonExpression<JsonNode> extends AbstractSimpleBinaryOperatorExpression<JsonNode> {
	public AbstractComparisonExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		super(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
	}

	protected abstract boolean test(int r);

	@Override
	protected JsonNode doEval(RuntimeLimits limits, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		int r = new JsonNodeComparator<>(jsonProvider).compare(lhs, rhs);
		return jsonProvider.createBoolean(test(r));
	}
}
