package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface PathOutput<JsonNode> {

	void emit(JsonNode out, Path<JsonNode> path) throws JsonQueryException;
}
