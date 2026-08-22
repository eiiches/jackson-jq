package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionCall<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final Expression<JsonNode> function;

	public ResolvedFunctionCall(String name, Expression<JsonNode> function) {
		this.name = name;
		this.function = function;
	}

	public Expression<JsonNode> function() {
		return function;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		function.apply(frame, in, path, output);
	}

	@Override
	public String toString() {
		return name + "()";
	}
}
