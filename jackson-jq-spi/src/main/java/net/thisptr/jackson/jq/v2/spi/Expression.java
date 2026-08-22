package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression<JsonNode> {

	// TODO: rename and fix; Expression may return multiple values
	default @Nullable JsonNode evaluateConstantExpr() {
		return null;
	}

	void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException;
}
