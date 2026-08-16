package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.ArrayList;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryUtils {

	public static <JsonNode> ArrayList<JsonNode> applyToArrayList(Expression expr, JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in) throws JsonQueryException {
		ArrayList<JsonNode> output = new ArrayList<>();
		expr.apply(jsonProvider, frame, in, output::add);
		return output;
	}

	public static <JsonNode> JsonNode applyToArrayNode(Expression expr, JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in) throws JsonQueryException {
		return JsonNodeUtils.asArrayNode(jsonProvider, applyToArrayList(expr, jsonProvider, frame, in));
	}
}
