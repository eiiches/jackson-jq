package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import net.thisptr.jackson.jq.v2.core.internal.operators.AlternativeOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ComplexAlternativeAssignment<JsonNode> extends ComplexAssignment<JsonNode> {
	public ComplexAlternativeAssignment(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(jsonProvider, lhs, rhs, new AlternativeOperator<>());
	}
}
