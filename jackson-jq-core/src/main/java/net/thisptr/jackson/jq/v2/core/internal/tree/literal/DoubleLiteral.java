package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class DoubleLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	private final double value;

	public DoubleLiteral(JsonProvider<JsonNode> jsonProvider, double value) {
		super(jsonProvider);
		this.value = value;
	}

	@Override
	public JsonNode value() {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}
}
