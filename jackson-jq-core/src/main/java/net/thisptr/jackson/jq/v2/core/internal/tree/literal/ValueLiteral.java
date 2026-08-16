package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class ValueLiteral implements Expression, AstNode {

	public abstract <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider);

	@Override
	public <JsonNode> @Nullable JsonNode evaluateConstantExpr(JsonProvider<JsonNode> jsonProvider) {
		return value(jsonProvider);
	}

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		output.emit(value(jsonProvider), null);
	}
}
