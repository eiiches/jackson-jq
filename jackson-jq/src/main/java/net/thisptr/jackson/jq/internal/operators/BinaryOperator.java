package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface BinaryOperator {
	JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	String image();
}
