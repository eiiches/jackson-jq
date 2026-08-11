package net.thisptr.jackson.jq.v2.core.path;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootPath<JsonNode> implements Path<JsonNode> {
	@SuppressWarnings("rawtypes")
	private static final RootPath INSTANCE = new RootPath<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> RootPath<JsonNode> getInstance() {
		return (RootPath<JsonNode>) INSTANCE;
	}

	private RootPath() {}

	@Override
	public void toJsonNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode out) throws JsonQueryException {
		return;
	}

	@Override
	public void get(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean permissive) throws JsonQueryException {
		output.emit(in, ipath);
	}

	@Override
	public JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		return mutation.apply(in);
	}
}
