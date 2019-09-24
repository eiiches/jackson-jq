package net.thisptr.jackson.jq.internal.misc;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JsonQueryUtils {

	public static ArrayList<JsonNode> applyToArrayList(final Expression expr, final Scope scope, final JsonNode in) throws JsonQueryException {
		final ArrayList<JsonNode> output = new ArrayList<>();
		expr.apply(scope, in, output::add);
		return output;
	}

	public static ArrayNode applyToArrayNode(final Expression expr, final Scope scope, final JsonNode in) throws JsonQueryException {
		return JsonNodeUtils.asArrayNode(scope.getObjectMapper(), applyToArrayList(expr, scope, in));
	}
}
