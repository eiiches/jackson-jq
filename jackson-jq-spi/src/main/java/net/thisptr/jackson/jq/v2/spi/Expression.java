package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression<JsonNode> {

	default @Nullable JsonNode evaluateConstantExpr() {
		return null;
	}

	default void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(frame, in, null, output, false);
	}

	void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
