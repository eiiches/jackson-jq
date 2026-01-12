package net.thisptr.jackson.jq.exception;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class JsonQueryUserException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	private Object value;

	public <JsonNode> JsonQueryUserException(final JsonProvider<JsonNode> jsonProvider, final JsonNode value) {
		super(jsonProvider.getNodeType(value) == JsonNodeType.STRING
				? jsonProvider.asText(value)
				: jsonProvider.toString(value));
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <JsonNode> JsonNode getMessageAsJsonNode(final JsonProvider<JsonNode> jsonProvider) {
		return (JsonNode) value;
	}
}
