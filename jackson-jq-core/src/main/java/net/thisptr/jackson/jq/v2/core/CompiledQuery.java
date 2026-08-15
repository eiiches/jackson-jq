package net.thisptr.jackson.jq.v2.core;

import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@FunctionalInterface
public interface CompiledQuery<JsonNode> {
	void apply(JsonNode in, PathOutput<JsonNode> output) throws JsonQueryException;
}
