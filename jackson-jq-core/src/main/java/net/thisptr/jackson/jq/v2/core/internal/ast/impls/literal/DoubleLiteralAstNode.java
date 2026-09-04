package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class DoubleLiteralAstNode extends ValueLiteralAstNode {
	private double value;

	public DoubleLiteralAstNode(double value) {
		this.value = value;
	}

	public double value() {
		return value;
	}

	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return JsonNodeUtils.asNumericNode(jsonProvider, value);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}
}
