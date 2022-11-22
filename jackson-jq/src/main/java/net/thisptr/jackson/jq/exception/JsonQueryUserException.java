package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class JsonQueryUserException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	private JsonNode value;

	public JsonQueryUserException(final JsonNode value) {
		super(value.isTextual() ? value.asText() : JsonNodeUtils.toString(value));
		this.value = value;
	}

	@Override
	public JsonNode getMessageAsJsonNode() {
		return value;
	}
}
