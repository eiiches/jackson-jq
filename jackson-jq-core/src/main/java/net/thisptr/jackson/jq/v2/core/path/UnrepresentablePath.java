package net.thisptr.jackson.jq.v2.core.path;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Marks a path lost mid-traversal (e.g. across a pipe stage that doesn't preserve one) while a
 * path-tracking evaluation is still in progress, as distinct from {@code null} meaning "not tracking
 * at all". Carries no message: every real consumer of a path (FieldAccess's emit* helpers, path(),
 * assignment operators) already knows how to build its own contextual error the moment it observes
 * this marker in place of a real path, so this class's own accessors should never normally be reached.
 */
public final class UnrepresentablePath<JsonNode> implements Path<JsonNode> {
	private static final UnrepresentablePath<?> INSTANCE = new UnrepresentablePath<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> UnrepresentablePath<JsonNode> getInstance() {
		return (UnrepresentablePath<JsonNode>) INSTANCE;
	}

	public static boolean isLost(@Nullable Path<?> path) {
		return path == null || path instanceof UnrepresentablePath;
	}

	private UnrepresentablePath() {}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		throw new JsonQueryException("Invalid path expression");
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive) throws JsonQueryException {
		throw new JsonQueryException("Invalid path expression");
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		throw new JsonQueryException("Invalid path expression");
	}
}
