package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Marks a path lost mid-traversal (e.g. across a pipe stage that doesn't preserve one) while a
 * path-tracking evaluation is still in progress, as distinct from {@link UntrackedPath}, which means
 * a value was never being tracked at all.
 *
 * @param <JsonNode> the JSON node type
 */
public final class UnrepresentablePath<JsonNode> extends Path<JsonNode> {
	private static final UnrepresentablePath<?> INSTANCE = new UnrepresentablePath<>();

	/**
	 * Returns the singleton instance of the unrepresentable path.
	 *
	 * @param <JsonNode> the JSON node type
	 * @return the unrepresentable path instance
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> UnrepresentablePath<JsonNode> getInstance() {
		return (UnrepresentablePath<JsonNode>) INSTANCE;
	}

	private UnrepresentablePath() {
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		throw new JsonQueryException("Invalid path expression");
	}

	@Override
	public @Nullable Path<JsonNode> getParentPath() {
		return null;
	}
}
