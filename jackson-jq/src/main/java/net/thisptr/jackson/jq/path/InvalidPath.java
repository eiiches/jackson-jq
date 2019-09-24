package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class InvalidPath implements Path {
	private final Path parent;
	private final JsonNode index;

	public InvalidPath(final Path parent, final JsonNode index) {
		this.parent = parent;
		this.index = index;
		if (parent == null)
			throw new NullPointerException("parent must not be null");
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		parent.toJsonNode(out);
		out.add(index);
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, final boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", in.getNodeType().toString().toLowerCase(), index.getNodeType().toString().toLowerCase()));
		}, permissive);
	}

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			throw new JsonQueryException(String.format("Cannot index %s with %s", in.getNodeType().toString().toLowerCase(), index.getNodeType().toString().toLowerCase()));
		}, makeParent);
	}
}
