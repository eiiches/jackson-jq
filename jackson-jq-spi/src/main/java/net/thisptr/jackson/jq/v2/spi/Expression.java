package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression<JsonNode> {

	default void apply(Scope<JsonNode> scope, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(scope, in, null, output, false);
	}

	void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
