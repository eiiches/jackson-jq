package net.thisptr.jackson.jq.v2.core.internal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FixedScopeQuery<JsonNode> implements Expression {
	public ExecutionStack<JsonNode>.@Nullable Frame frame;
	public Expression query;

	public FixedScopeQuery(ExecutionStack<JsonNode>.@Nullable Frame frame, Expression query) {
		this.frame = frame;
		this.query = query;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame unused, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		query.apply((JsonProvider) jsonProvider, (ExecutionStack.Frame) frame, (JsonNode) in, (Path) path, (PathOutput) output, requirePath);
	}
}
