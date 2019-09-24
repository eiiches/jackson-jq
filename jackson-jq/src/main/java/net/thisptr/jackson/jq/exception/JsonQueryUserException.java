package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonQueryUserException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryUserException(final JsonNode msg) {
		super(msg.asText());
	}
}
