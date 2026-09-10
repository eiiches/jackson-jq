package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class NumericLiteral<JsonNode> extends AbstractValueLiteral<JsonNode> {
	private final JsonNode value;

	public NumericLiteral(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		super(jsonProvider);
		this.value = value;
	}

	@Override
	public JsonNode value() {
		return value;
	}

	@Override
	public String toString() {
		return jsonProvider.format(value);
	}
}
