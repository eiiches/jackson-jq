package net.thisptr.jackson.jq.v2.core.path;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class InvalidPath<JsonNode> implements Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode index;
	private final @Nullable Version version;

	public InvalidPath(Path<JsonNode> parent, JsonNode index) {
		this(parent, index, null);
	}

	public InvalidPath(Path<JsonNode> parent, JsonNode index, @Nullable Version version) {
		this.parent = parent;
		this.index = index;
		this.version = version;
		if (parent == null)
			throw new NullPointerException("parent must not be null");
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, index);
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, index));
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, index));
		}, makeParent);
	}
}
