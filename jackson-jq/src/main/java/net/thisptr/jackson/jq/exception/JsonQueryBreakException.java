package net.thisptr.jackson.jq.exception;

import net.thisptr.jackson.jq.JsonProvider;

public class JsonQueryBreakException extends JsonQueryException {
	private static final long serialVersionUID = -6066878919494380889L;

	private final String name;

	public JsonQueryBreakException(final String name) {
		super("break");
		this.name = name;
	}

	@Override
	public <JsonNode> JsonNode getMessageAsJsonNode(final JsonProvider<JsonNode> jsonProvider) {
		final JsonNode object = jsonProvider.createObject();
		jsonProvider.set(object, "__jq", jsonProvider.createInt(0));
		return object;
	}

	public String name() {
		return name;
	}
}
