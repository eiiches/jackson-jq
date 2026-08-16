package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression {

	default <JsonNode> @Nullable JsonNode evaluateConstantExpr(JsonProvider<JsonNode> jsonProvider) {
		return null;
	}

	default <JsonNode> void apply(JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		throw new UnsupportedOperationException("Expression.apply requires a JsonProvider");
	}

	default <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(jsonProvider, frame, in, null, output, false);
	}

	default <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		apply(jsonProvider, null, in, ipath, output, requirePath);
	}

	<JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
