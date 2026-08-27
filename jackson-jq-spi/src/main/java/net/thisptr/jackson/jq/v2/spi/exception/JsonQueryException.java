package net.thisptr.jackson.jq.v2.spi.exception;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class JsonQueryException extends RuntimeException {
	private static final long serialVersionUID = -7241258446595502920L;

	public JsonQueryException(String msg) {
		super(msg);
	}

	public JsonQueryException(Throwable e) {
		super(e);
	}

	public JsonQueryException(String msg, Throwable rootCause) {
		super(msg, rootCause);
	}

	public <JsonNode> JsonNode toJsonNode(JsonProvider<JsonNode> jsonProvider) {
		String message = getMessage();
		return message == null ? jsonProvider.createNull() : jsonProvider.createString(message);
	}
}
