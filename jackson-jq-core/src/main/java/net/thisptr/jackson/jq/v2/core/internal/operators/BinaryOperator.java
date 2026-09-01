package net.thisptr.jackson.jq.v2.core.internal.operators;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public interface BinaryOperator<JsonNode> {
	JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	String image();
}
