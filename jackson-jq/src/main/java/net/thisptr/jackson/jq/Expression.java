package net.thisptr.jackson.jq;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Expression<JsonNode> {

	default void apply(Scope<JsonNode> scope, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(scope, in, null, output, false);
	}

	void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
