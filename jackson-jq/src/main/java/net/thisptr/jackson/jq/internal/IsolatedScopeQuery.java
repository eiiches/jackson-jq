package net.thisptr.jackson.jq.internal;

import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class IsolatedScopeQuery extends JsonQuery {
	private JsonQuery q;

	public IsolatedScopeQuery(final JsonQuery q) {
		this.q = q;
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		final Scope isolatedScope = new Scope(scope);
		return q.apply(isolatedScope, in);
	}

	@Override
	public String toString() {
		return q.toString();
	}
}
