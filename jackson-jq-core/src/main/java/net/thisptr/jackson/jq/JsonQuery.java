package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JsonQuery {
	private final Expression expr;

	private JsonQuery(final Expression expr) {
		this.expr = expr;
	}

	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		expr.apply(scope, in, output);
	}

	public static JsonQuery compile(final String path, final Version version) throws JsonQueryException {
		return new JsonQuery(new IsolatedScopeQuery(ExpressionParser.compile(path, version)));
	}

	@Override
	public String toString() {
		return expr.toString();
	}
}
