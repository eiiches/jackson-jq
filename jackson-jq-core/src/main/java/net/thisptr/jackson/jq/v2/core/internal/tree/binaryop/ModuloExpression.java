package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.operators.ModuloOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;

public class ModuloExpression<JsonNode> extends SimpleBinaryOperatorExpression<JsonNode> {
	public ModuloExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		this(jsonProvider, lhs, rhs, null);
	}

	public ModuloExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs, @Nullable Version version) {
		super(jsonProvider, lhs, rhs, new ModuloOperator<>(version));
	}
}
