package net.thisptr.jackson.jq.v2.spi.path;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Represents the root of a jq path expression.
 *
 * @param <JsonNode> the JSON node type
 */
public final class RootPath<JsonNode> extends Path<JsonNode> {
	private static final RootPath<?> INSTANCE = new RootPath<>();

	/**
	 * Returns the singleton instance of the root path.
	 *
	 * @param <JsonNode> the JSON node type
	 * @return the root path instance
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> RootPath<JsonNode> getInstance() {
		return (RootPath<JsonNode>) INSTANCE;
	}

	private RootPath() {
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		return new ArrayList<>();
	}

	@Override
	public @Nullable Path<JsonNode> getParentPath() {
		return null;
	}
}
