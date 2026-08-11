package net.thisptr.jackson.jq.v2.core.internal;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FixedScopeQuery<JsonNode> implements Expression<JsonNode> {
	public Scope<JsonNode> scope;
	public Expression<JsonNode> query;

	public FixedScopeQuery(final Scope<JsonNode> scope, final Expression<JsonNode> query) {
		this.scope = scope;
		this.query = query;
	}

	@Override
	public void apply(Scope<JsonNode> unused, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		query.apply(scope, in, path, output, requirePath);
	}
}
