package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class LongLiteral<JsonNode> extends ValueLiteral<JsonNode> {
	private final long value;

	public LongLiteral(JsonProvider<JsonNode> jsonProvider, long value) {
		super(jsonProvider);
		this.value = value;
	}

	@Override
	public JsonNode value() {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}
}
