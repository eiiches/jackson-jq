package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@FunctionalInterface
public interface Function {

	<JsonNode> void apply(JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException;
}

