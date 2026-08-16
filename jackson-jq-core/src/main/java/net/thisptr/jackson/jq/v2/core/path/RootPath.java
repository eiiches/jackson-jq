package net.thisptr.jackson.jq.v2.core.path;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootPath<JsonNode> implements Path<JsonNode> {
	private static final RootPath<?> INSTANCE = new RootPath<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> RootPath<JsonNode> getInstance() {
		return (RootPath<JsonNode>) INSTANCE;
	}

	private RootPath() {}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		return;
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		output.emit(in, ipath);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return Objects.requireNonNull(mutation.apply(in));
	}
}
