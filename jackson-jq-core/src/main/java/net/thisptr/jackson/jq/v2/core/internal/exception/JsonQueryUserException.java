package net.thisptr.jackson.jq.v2.core.internal.exception;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryUserException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	private Object value;

	public <JsonNode> JsonQueryUserException(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		super(jsonProvider.getNodeType(value) == JsonNodeType.STRING
				? jsonProvider.getString(value)
				: jsonProvider.format(value));
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <JsonNode> JsonNode toJson(JsonProvider<JsonNode> jsonProvider) {
		return (JsonNode) value;
	}
}
