package net.thisptr.jackson.jq.internal;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class FixedScopeQuery implements Expression {
	public Scope scope;
	public Expression query;

	public FixedScopeQuery(final Scope scope, final Expression query) {
		this.scope = scope;
		this.query = query;
	}

	@Override
	public void apply(Scope unused, JsonNode in, Path path, PathOutput output, boolean requirePath) throws JsonQueryException {
		query.apply(scope, in, path, output, requirePath);
	}
}
