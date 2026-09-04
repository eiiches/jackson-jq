package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class LongLiteralAstNode extends ValueLiteralAstNode {
	private long value;

	public LongLiteralAstNode(long value) {
		this.value = value;
	}

	public long value() {
		return value;
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
