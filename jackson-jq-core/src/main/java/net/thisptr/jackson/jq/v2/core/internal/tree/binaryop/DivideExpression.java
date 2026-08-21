package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.operators.DivideOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;

public class DivideExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public DivideExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		this(jsonProvider, lhs, rhs, null);
	}

	public DivideExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs, @Nullable Version version) {
		super(jsonProvider, lhs, rhs, new DivideOperator<>(version));
	}
}
