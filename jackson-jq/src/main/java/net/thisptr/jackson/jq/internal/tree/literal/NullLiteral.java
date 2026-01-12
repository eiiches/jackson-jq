package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.JsonProvider;

public class NullLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	@Override
	public JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createNull();
	}

	@Override
	public String toString() {
		return "null";
	}
}
