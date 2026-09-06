package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class NullLiteralAstNode extends AbstractValueLiteralAstNode {
	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createNull();
	}

	@Override
	public String toString() {
		return "null";
	}
}
