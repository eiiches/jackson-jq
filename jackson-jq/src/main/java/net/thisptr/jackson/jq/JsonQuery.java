package net.thisptr.jackson.jq;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JsonQuery {
	private final Expression expr;

	public JsonQuery(final Expression expr) {
		this.expr = expr;
	}

	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> output = new ArrayList<>();
		expr.apply(scope, in, output::add);
		return output;
	}

	public static JsonQuery compile(final String path) throws JsonQueryException {
		return new JsonQuery(new IsolatedScopeQuery(ExpressionParser.compile(path, Versions.JQ_1_5)));
	}

	public static JsonQuery compile(final String path, final Version version) throws JsonQueryException {
		return new JsonQuery(new IsolatedScopeQuery(ExpressionParser.compile(path, version)));
	}

	@Override
	public String toString() {
		return expr.toString();
	}
}
