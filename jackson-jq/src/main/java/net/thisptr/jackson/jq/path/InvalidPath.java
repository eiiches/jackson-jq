package net.thisptr.jackson.jq.path;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class InvalidPath<JsonNode> implements Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode index;

	public InvalidPath(final Path<JsonNode> parent, final JsonNode index) {
		this.parent = parent;
		this.index = index;
		if (parent == null)
			throw new NullPointerException("parent must not be null");
	}

	@Override
	public void toJsonNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, index);
	}

	@Override
	public void get(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", jsonProvider.getNodeType(in).toString().toLowerCase(), jsonProvider.getNodeType(index).toString().toLowerCase()));
		}, permissive);
	}

	@Override
	public JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", jsonProvider.getNodeType(in).toString().toLowerCase(), jsonProvider.getNodeType(index).toString().toLowerCase()));
		}, makeParent);
	}
}
