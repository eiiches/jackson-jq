package net.thisptr.jackson.jq.v2.core.internal.exception;

import com.google.errorprone.annotations.Var;

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
		@Var JsonNode object = jsonProvider.createObject();
		object = jsonProvider.set(object, "__jq", jsonProvider.createNumber(0));
		return object;
	}

	public String name() {
		return name;
	}
}
