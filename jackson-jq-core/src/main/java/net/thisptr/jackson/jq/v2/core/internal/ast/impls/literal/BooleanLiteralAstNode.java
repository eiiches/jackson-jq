package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class BooleanLiteralAstNode extends ValueLiteralAstNode {
	private boolean value;

	public BooleanLiteralAstNode(boolean value) {
		this.value = value;
	}

	public boolean value() {
		return value;
	}

	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createBoolean(value);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
