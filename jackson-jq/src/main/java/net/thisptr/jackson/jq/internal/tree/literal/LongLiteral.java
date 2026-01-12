package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class LongLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	private long value;

	public LongLiteral(final long value) {
		this.value = value;
	}

	@Override
	public JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}
}
