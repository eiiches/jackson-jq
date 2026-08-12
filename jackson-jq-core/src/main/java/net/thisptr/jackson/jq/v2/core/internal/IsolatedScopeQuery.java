package net.thisptr.jackson.jq.v2.core.internal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class IsolatedScopeQuery<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> q;

	public IsolatedScopeQuery(Expression<JsonNode> q) {
		this.q = q;
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Scope<JsonNode> isolatedScope = Scope.newChildScope(scope);
		q.apply(isolatedScope, in, path, output, requirePath);
	}

	@Override
	public String toString() {
		return q.toString();
	}
}
