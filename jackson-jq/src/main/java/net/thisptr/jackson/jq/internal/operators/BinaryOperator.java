package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface BinaryOperator<JsonNode> {
	JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	String image();
}
