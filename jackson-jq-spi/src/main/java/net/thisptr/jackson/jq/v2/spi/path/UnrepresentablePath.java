package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Marks a path that went stale mid-traversal (e.g. across a pipe stage that doesn't preserve one)
 * while a path-tracking evaluation is still in progress, as distinct from {@link UntrackedPath},
 * which means a value was never being tracked at all.
 * <p>
 * jq never truly discards a position: it keeps the last path it tracked together with the value that
 * was there, and a later path step, {@code path/1} or an assignment resumes from that position when
 * the value it is looking at is still the one recorded here. This class carries the same pair, so
 * that a consumer can decide between resuming and reporting an invalid path expression.
 *
 * @param <JsonNode> the JSON node type
 */
public final class UnrepresentablePath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> lastValidPath;
	private final JsonNode valueAtLastValidPath;

	/**
	 * Returns a path that went stale at {@code path}, where {@code value} was.
	 * <p>
	 * A path that is already stale is returned unchanged: jq keeps the position it first lost, so
	 * every later stage that fails to produce a path leaves that position alone.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param path the last path that was tracked
	 * @param value the value at {@code path}
	 * @return the stale path
	 */
	public static <JsonNode> Path<JsonNode> of(Path<JsonNode> path, JsonNode value) {
		if (path instanceof UnrepresentablePath)
			return path;
		return new UnrepresentablePath<>(path, value);
	}

	private UnrepresentablePath(Path<JsonNode> lastValidPath, JsonNode valueAtLastValidPath) {
		this.lastValidPath = lastValidPath;
		this.valueAtLastValidPath = valueAtLastValidPath;
	}

	/**
	 * Returns the last path that was tracked before this one went stale.
	 *
	 * @return the last valid path
	 */
	public Path<JsonNode> getLastValidPath() {
		return lastValidPath;
	}

	/**
	 * Returns the value that was at {@link #getLastValidPath()} when this path went stale.
	 *
	 * @return the value at the last valid path
	 */
	public JsonNode getValueAtLastValidPath() {
		return valueAtLastValidPath;
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
