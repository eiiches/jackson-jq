package net.thisptr.jackson.jq.internal;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class IsolatedScopeQuery implements Expression {
	private Expression q;

	public IsolatedScopeQuery() {

	}

	public Expression getQ() {
		return q;
	}

	public void setQ(Expression q) {
		this.q = q;
	}

	public IsolatedScopeQuery(final Expression q) {
		this.q = q;
	}

	@Override
	public void apply(Scope scope, JsonNode in, Path path, PathOutput output, boolean requirePath) throws JsonQueryException {
		final Scope isolatedScope = Scope.newChildScope(scope);
		q.apply(isolatedScope, in, path, output, requirePath);
	}

	@Override
	public String toString() {
		return q.toString();
	}
}
