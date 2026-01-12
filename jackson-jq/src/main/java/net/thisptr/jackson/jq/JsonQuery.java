package net.thisptr.jackson.jq;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JsonQuery<JsonNode> {
	private final Expression<JsonNode> expr;

	private JsonQuery(final Expression<JsonNode> expr) {
		this.expr = expr;
	}

	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Output<JsonNode> output) throws JsonQueryException {
		expr.apply(scope, in, output);
	}

	@SuppressWarnings("unchecked")
	public static <JsonNode> JsonQuery<JsonNode> compile(final String path, final Version version) throws JsonQueryException {
		return new JsonQuery<>(new IsolatedScopeQuery<>((Expression<JsonNode>) ExpressionParser.compile(path, version)));
	}

	@Override
	public String toString() {
		return expr.toString();
	}
}
