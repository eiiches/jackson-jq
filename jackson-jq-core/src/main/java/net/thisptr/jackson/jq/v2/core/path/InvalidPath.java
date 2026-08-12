package net.thisptr.jackson.jq.v2.core.path;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class InvalidPath<JsonNode> implements Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode index;

	public InvalidPath(Path<JsonNode> parent, JsonNode index) {
		this.parent = parent;
		this.index = index;
		if (parent == null)
			throw new NullPointerException("parent must not be null");
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, index);
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", jsonProvider.getNodeType(in).toString().toLowerCase(), jsonProvider.getNodeType(index).toString().toLowerCase()));
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", jsonProvider.getNodeType(in).toString().toLowerCase(), jsonProvider.getNodeType(index).toString().toLowerCase()));
		}, makeParent);
	}
}
