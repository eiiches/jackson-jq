package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class RootPath implements Path {
	private static final RootPath INSTANCE = new RootPath();

	public static RootPath getInstance() {
		return INSTANCE;
	}

	private RootPath() {}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, final boolean permissive) throws JsonQueryException {
		output.emit(in, ipath);
	}

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return mutation.apply(in);
	}
}
