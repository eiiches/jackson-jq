package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.ArrayList;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryUtils {

	public static <JsonNode> ArrayList<JsonNode> applyToArrayList(Expression<JsonNode> expr, Scope<JsonNode> scope, JsonNode in) throws JsonQueryException {
		ArrayList<JsonNode> output = new ArrayList<>();
		expr.apply(scope, in, output::add);
		return output;
	}

	public static <JsonNode> JsonNode applyToArrayNode(Expression<JsonNode> expr, Scope<JsonNode> scope, JsonNode in) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		return JsonNodeUtils.asArrayNode(jsonProvider, applyToArrayList(expr, scope, in));
	}
}
