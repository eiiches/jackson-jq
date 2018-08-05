package net.thisptr.jackson.jq.internal;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class IsolatedScopeQuery implements Expression {
	private Expression q;

	public IsolatedScopeQuery(final Expression q) {
		this.q = q;
	}

	@Override
	public void apply(Scope scope, JsonNode in, Output output) throws JsonQueryException {
		final Scope isolatedScope = Scope.newChildScope(scope);
		q.apply(isolatedScope, in, output);
	}

	@Override
	public String toString() {
		return q.toString();
	}
}
