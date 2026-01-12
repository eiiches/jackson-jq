package net.thisptr.jackson.jq;

import java.util.List;


import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Function<JsonNode> {

	void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException;
}
