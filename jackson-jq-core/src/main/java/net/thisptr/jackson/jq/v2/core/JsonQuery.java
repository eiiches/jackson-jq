package net.thisptr.jackson.jq.v2.core;

import net.thisptr.jackson.jq.v2.core.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQuery<JsonNode> {
	private final Expression<JsonNode> expr;

	private JsonQuery(Expression<JsonNode> expr) {
		this.expr = expr;
	}

	public void apply(Scope<JsonNode> scope, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		expr.apply(scope, in, output);
	}

	@SuppressWarnings("unchecked")
	public static <JsonNode> JsonQuery<JsonNode> compile(String path, Version version) throws JsonQueryException {
		return new JsonQuery<>(new IsolatedScopeQuery<>((Expression<JsonNode>) ExpressionParser.compile(path, version)));
	}

	@Override
	public String toString() {
		return expr.toString();
	}
}
