package net.thisptr.jackson.jq.v2.spi.path;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public interface Path<JsonNode> {

	void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException;

	void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException;

	interface Mutation<JsonNode> {
		@Nullable JsonNode apply(@Nullable JsonNode node) throws JsonQueryException;
	}

	default JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation) throws JsonQueryException {
		return mutate(jsonProvider, in, mutation, true);
	}

	JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException;
}
