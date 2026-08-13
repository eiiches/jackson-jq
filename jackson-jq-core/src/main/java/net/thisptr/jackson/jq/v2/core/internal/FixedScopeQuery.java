package net.thisptr.jackson.jq.v2.core.internal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FixedScopeQuery<JsonNode> implements Expression {
	public Scope<JsonNode> scope;
	public Expression query;

	public FixedScopeQuery(Scope<JsonNode> scope, Expression query) {
		this.scope = scope;
		this.query = query;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> unused, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((JsonNode) in, (Path) path, (PathOutput) output, requirePath);
	}

	private void applyInternal(JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		query.apply(scope, in, path, output, requirePath);
	}
}
