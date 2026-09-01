package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class NullLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	public NullLiteral(JsonProvider<JsonNode> jsonProvider) {
		super(jsonProvider);
	}

	@Override
	public JsonNode value() {
		return jsonProvider.createNull();
	}

	@Override
	public String toString() {
		return "null";
	}
}
