package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;

public class OrOperator implements BinaryOperator {

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final boolean r = JsonNodeUtils.asBoolean(lhs) || JsonNodeUtils.asBoolean(rhs);
		return BooleanNode.valueOf(r);
	}

	@Override
	public String image() {
		return "or";
	}
}
