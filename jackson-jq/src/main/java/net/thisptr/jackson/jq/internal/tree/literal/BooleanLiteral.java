package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.JsonProvider;

public class BooleanLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	private boolean value;

	public BooleanLiteral(final boolean value) {
		this.value = value;
	}

	@Override
	public JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createBoolean(value);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
