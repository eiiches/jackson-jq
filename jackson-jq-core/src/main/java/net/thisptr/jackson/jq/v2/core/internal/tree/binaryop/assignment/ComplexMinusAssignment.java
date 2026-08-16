package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.MinusOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexMinusAssignment<JsonNode> extends ComplexAssignment<JsonNode> {
	public ComplexMinusAssignment(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(jsonProvider, lhs, rhs, new MinusOperator<>());
	}
}
