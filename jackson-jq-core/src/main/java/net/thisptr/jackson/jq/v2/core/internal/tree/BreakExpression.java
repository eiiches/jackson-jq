package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BreakExpression<JsonNode> implements Expression<StackFrame, JsonNode> {
	private final String name;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ZERO;
	}

	public BreakExpression(String name) {
		this.name = name;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		throw new JsonQueryBreakException(name);
	}

	@Override
	public String toString() {
		return "break $" + name;
	}
}
