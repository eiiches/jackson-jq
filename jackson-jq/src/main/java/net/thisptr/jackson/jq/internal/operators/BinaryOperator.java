package net.thisptr.jackson.jq.internal.operators;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface BinaryOperator {
	JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	String image();
}
