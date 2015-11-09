package net.thisptr.jackson.jq.internal;

import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class FixedScopeQuery extends JsonQuery {
	public Scope scope;
	public JsonQuery query;

	public FixedScopeQuery(final Scope scope, final JsonQuery query) {
		this.scope = scope;
		this.query = query;
	}

	@Override
	public List<JsonNode> apply(final Scope _, final JsonNode in) throws JsonQueryException {
		return query.apply(scope, in);
	}
}