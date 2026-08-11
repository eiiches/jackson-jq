package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.ArrayList;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

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
