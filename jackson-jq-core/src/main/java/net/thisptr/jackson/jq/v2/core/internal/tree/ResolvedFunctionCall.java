package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionCall<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final Function<JsonNode> function;

	public ResolvedFunctionCall(String name, Function<JsonNode> function) {
		this.name = name;
		this.function = function;
	}

	public Function<JsonNode> function() {
		return function;
	}

	@Override
	public void apply(@Nullable StackFrame<JsonNode> frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		function.apply(frame, in, path, output);
	}

	@Override
	public String toString() {
		return name + "()";
	}
}
