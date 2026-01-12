package net.thisptr.jackson.jq;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface PathOutput<JsonNode> {

	void emit(JsonNode out, Path<JsonNode> path) throws JsonQueryException;
}
