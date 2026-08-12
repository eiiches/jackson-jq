package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface PathOutput<JsonNode> {

	void emit(JsonNode out, @Nullable Path<JsonNode> path) throws JsonQueryException;
}
