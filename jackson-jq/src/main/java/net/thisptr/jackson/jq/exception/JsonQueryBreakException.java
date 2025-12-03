package net.thisptr.jackson.jq.exception;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

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
