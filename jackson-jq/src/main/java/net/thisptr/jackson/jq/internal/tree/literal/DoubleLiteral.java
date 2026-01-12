package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class DoubleLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	private double value;

	public DoubleLiteral(final double value) {
		this.value = value;
	}

	@Override
	public JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}
}
