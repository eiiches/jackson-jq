package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class AlternativeOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return JsonNodeUtils.asBoolean(jsonProvider, lhs) ? lhs : rhs;
	}

	@Override
	public String image() {
		return "//";
	}
}
