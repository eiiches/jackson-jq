package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression<JsonNode> {

	// TODO: rename and fix; Expression may return multiple values
	default @Nullable JsonNode evaluateConstantExpr() {
		return null;
	}

	// TODO: reduce the number of interface methods
	default void apply(@Nullable StackFrame frame, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(frame, in, null, output, false);
	}

	// TODO: remove requirePath
	void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
