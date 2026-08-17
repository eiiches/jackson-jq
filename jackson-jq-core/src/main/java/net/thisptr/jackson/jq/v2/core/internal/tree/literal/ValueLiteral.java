package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class ValueLiteral<JsonNode> implements Expression<JsonNode> {
	protected final JsonProvider<JsonNode> jsonProvider;

	protected ValueLiteral(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public abstract JsonNode value();

	@Override
	public @Nullable JsonNode evaluateConstantExpr() {
		return value();
	}

	@Override
	public void apply(@Nullable StackFrame<JsonNode> frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		output.emit(value(), null);
	}
}
