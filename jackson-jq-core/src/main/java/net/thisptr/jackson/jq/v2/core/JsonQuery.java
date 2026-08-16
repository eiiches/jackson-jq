package net.thisptr.jackson.jq.v2.core;

import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@FunctionalInterface
public interface JsonQuery<JsonNode> {
	void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, PathOutput<JsonNode> output) throws JsonQueryException;

	default void apply(JsonNode in, PathOutput<JsonNode> output) throws JsonQueryException {
		apply(in, JsonQueryBindings.empty(), output);
	}
}
