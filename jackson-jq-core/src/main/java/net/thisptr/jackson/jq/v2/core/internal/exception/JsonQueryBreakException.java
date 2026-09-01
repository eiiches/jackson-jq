package net.thisptr.jackson.jq.v2.core.internal.exception;

import java.util.Collections;

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
	public <JsonNode> JsonNode toJson(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createObject(Collections.singletonMap("__jq", jsonProvider.createNumber(0)));
	}

	public String name() {
		return name;
	}
}
