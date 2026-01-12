package net.thisptr.jackson.jq.internal.misc;

import java.util.ArrayList;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JsonQueryUtils {

	public static <JsonNode> ArrayList<JsonNode> applyToArrayList(final Expression<JsonNode> expr, final Scope<JsonNode> scope, final JsonNode in) throws JsonQueryException {
		final ArrayList<JsonNode> output = new ArrayList<>();
		expr.apply(scope, in, output::add);
		return output;
	}

	public static <JsonNode> JsonNode applyToArrayNode(final Expression<JsonNode> expr, final Scope<JsonNode> scope, final JsonNode in) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		return JsonNodeUtils.asArrayNode(jsonProvider, applyToArrayList(expr, scope, in));
	}
}
