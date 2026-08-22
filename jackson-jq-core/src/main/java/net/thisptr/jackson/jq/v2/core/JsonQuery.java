package net.thisptr.jackson.jq.v2.core;

import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@FunctionalInterface
public interface JsonQuery<JsonNode> {
	void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, Output<JsonNode> output) throws JsonQueryException;

	default void apply(JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(in, JsonQueryBindings.empty(), output);
	}
}
