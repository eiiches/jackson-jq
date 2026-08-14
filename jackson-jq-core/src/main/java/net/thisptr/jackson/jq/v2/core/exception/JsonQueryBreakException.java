package net.thisptr.jackson.jq.v2.core.exception;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryBreakException extends JsonQueryException {
	private static final long serialVersionUID = -6066878919494380889L;

	private final String name;

	public JsonQueryBreakException(String name) {
		super("break");
		this.name = name;
	}

	@Override
	public <JsonNode> JsonNode getMessageAsJsonNode(JsonProvider<JsonNode> jsonProvider) {
		JsonNode object = jsonProvider.createObject();
		jsonProvider.set(object, "__jq", jsonProvider.createNumber(0));
		return object;
	}

	public String name() {
		return name;
	}
}
