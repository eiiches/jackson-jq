package net.thisptr.jackson.jq.v2.core.internal.operators;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

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
