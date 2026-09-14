package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Marks a value as not being tracked through a path-tracking evaluation at all, as distinct from
 * {@link UnrepresentablePath}, which means a path was being tracked but was lost mid-traversal.
 *
 * @param <JsonNode> the JSON node type
 */
public final class UntrackedPath<JsonNode> extends Path<JsonNode> {
	private static final UntrackedPath<?> INSTANCE = new UntrackedPath<>();

	/**
	 * Returns the singleton instance of the untracked path.
	 *
	 * @param <JsonNode> the JSON node type
	 * @return the untracked path instance
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> UntrackedPath<JsonNode> getInstance() {
		return (UntrackedPath<JsonNode>) INSTANCE;
	}

	private UntrackedPath() {
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		throw new JsonQueryException("Invalid path expression");
	}

	@Override
	public @Nullable Path<JsonNode> getParentPath() {
		return null;
	}

	@Override
	public Path<JsonNode> appendKey(String key) {
		return this;
	}

	@Override
	public Path<JsonNode> appendIndex(int index) {
		return this;
	}

	@Override
	public Path<JsonNode> appendIndex(JsonProvider<JsonNode> jsonProvider, JsonNode index) {
		return this;
	}

	@Override
	public Path<JsonNode> appendIndexRange(JsonProvider<JsonNode> jsonProvider, JsonNode start, JsonNode end) {
		return this;
	}

	@Override
	public Path<JsonNode> appendIndexOf(JsonProvider<JsonNode> jsonProvider, JsonNode searchSequence) {
		return this;
	}

	@Override
	public Path<JsonNode> appendInvalid(JsonNode index) {
		return this;
	}
}
