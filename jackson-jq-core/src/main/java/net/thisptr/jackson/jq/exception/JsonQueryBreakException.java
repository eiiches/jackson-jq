package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonQueryBreakException extends JsonQueryException {
	private static final long serialVersionUID = -6066878919494380889L;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final String name;

	public JsonQueryBreakException(final String name) {
		super("break");
		this.name = name;
	}

	@Override
	public JsonNode getMessageAsJsonNode() {
		final ObjectNode object = MAPPER.createObjectNode();
		object.set("__jq", IntNode.valueOf(0));
		return object;
	}

	public String name() {
		return name;
	}
}
