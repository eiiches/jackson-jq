package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class BooleanLiteral<JsonNode> extends AbstractValueLiteral<JsonNode> {
	private final boolean value;

	public BooleanLiteral(JsonProvider<JsonNode> jsonProvider, boolean value) {
		super(jsonProvider);
		this.value = value;
	}

	@Override
	public JsonNode value() {
		return jsonProvider.createBoolean(value);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
