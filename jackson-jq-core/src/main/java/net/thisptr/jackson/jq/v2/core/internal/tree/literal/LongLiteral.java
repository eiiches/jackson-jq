package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class LongLiteral extends ValueLiteral {
	private long value;

	public LongLiteral(long value) {
		this.value = value;
	}

	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}
}
