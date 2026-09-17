package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * A {@link MeteredOutputExpression} over an argument that constant-folded, still answering as the
 * {@link ConstantExpression} it wraps.
 * <p>
 * A {@code Function} may read {@link #getConstantResults()} at bind time instead of ever calling
 * {@link #apply}; nothing is charged then, and nothing should be -- those values were produced once, at
 * compile time. Whatever the function goes on to emit at runtime is charged to the call site that produced
 * it, like any other expression.
 *
 * @param <JsonNode> the JSON node type
 */
final class MeteredConstantOutputExpression<JsonNode> extends MeteredOutputExpression<JsonNode> implements ConstantExpression<StackFrame, JsonNode> {

	MeteredConstantOutputExpression(Expression<StackFrame, JsonNode> inner, int index) {
		super(inner, index);
	}

	@Override
	public List<JsonNode> getConstantResults() {
		@SuppressWarnings("unchecked")
		ConstantExpression<StackFrame, JsonNode> constant = (ConstantExpression<StackFrame, JsonNode>) inner;
		return constant.getConstantResults();
	}
}
