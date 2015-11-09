package net.thisptr.jackson.jq.exception;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonQueryUserException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryUserException(final List<JsonNode> msgs) {
		super((msgs.isEmpty() ? "" : msgs.get(0).asText()));
	}
}
