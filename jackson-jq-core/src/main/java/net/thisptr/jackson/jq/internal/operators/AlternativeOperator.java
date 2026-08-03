package net.thisptr.jackson.jq.internal.operators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class AlternativeOperator implements BinaryOperator {
	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		return JsonNodeUtils.asBoolean(lhs) ? lhs : rhs;
	}

	@Override
	public String image() {
		return "//";
	}
}
