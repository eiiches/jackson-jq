package net.thisptr.jackson.jq.path;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface Path<JsonNode> {

	void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException;

	void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException;

	interface Mutation<JsonNode> {
		JsonNode apply(JsonNode node) throws JsonQueryException;
	}

	default JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation) throws JsonQueryException {
		return mutate(jsonProvider, in, mutation, true);
	}

	JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException;
}
