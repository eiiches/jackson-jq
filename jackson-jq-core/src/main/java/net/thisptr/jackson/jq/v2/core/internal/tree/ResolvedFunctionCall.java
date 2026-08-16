package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionCall<JsonNode> implements Expression {
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
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame frame, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		((Function) function).apply(frame, in, path, output);
	}

	@Override
	public String toString() {
		return name + "()";
	}
}
