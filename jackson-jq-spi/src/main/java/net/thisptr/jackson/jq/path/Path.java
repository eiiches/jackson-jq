package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface Path {

	void toJsonNode(ArrayNode out) throws JsonQueryException;

	void get(JsonNode in, Path ipath, PathOutput output, boolean permissive) throws JsonQueryException;

	interface Mutation {
		JsonNode apply(JsonNode node) throws JsonQueryException;
	}

	default JsonNode mutate(final JsonNode in, final Mutation mutation) throws JsonQueryException {
		return mutate(in, mutation, true);
	}

	JsonNode mutate(JsonNode in, Mutation mutation, boolean makeParent) throws JsonQueryException;
}
