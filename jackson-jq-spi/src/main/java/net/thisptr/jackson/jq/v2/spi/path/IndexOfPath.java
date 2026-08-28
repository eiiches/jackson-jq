package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Represents an array index-of search step in a path (e.g. from {@code index/1} or {@code indices/1}).
 *
 * @param <JsonNode> the JSON node type
 */
public final class IndexOfPath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode searchSequence;

	/**
	 * Creates an {@code IndexOfPath} with the given parent and search sequence.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider used to validate the search sequence
	 * @param parent the parent path
	 * @param searchSequence the target search sequence to search for
	 * @return a new {@code IndexOfPath}
	 * @throws JsonQueryException if {@code searchSequence} is not an array
	 */
	static <JsonNode> IndexOfPath<JsonNode> of(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> parent, JsonNode searchSequence) {
		if (jsonProvider.getNodeType(searchSequence) != JsonNodeType.ARRAY)
			throw new JsonQueryException("Array index-of search sequence must be an array");
		return new IndexOfPath<>(parent, searchSequence);
	}

	/**
	 * Creates a new {@code IndexOfPath}.
	 *
	 * @param parent the parent path
	 * @param searchSequence the target search sequence to search for
	 */
	private IndexOfPath(Path<JsonNode> parent, JsonNode searchSequence) {
		this.parent = parent;
		this.searchSequence = searchSequence;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		List<JsonNode> result = parent.toJsonList(jsonProvider);
		result.add(searchSequence);
		return result;
	}

	/**
	 * Returns the parent path.
	 *
	 * @return the parent path
	 */
	@Override
	public Path<JsonNode> getParentPath() {
		return parent;
	}

	/**
	 * Returns the target search sequence node.
	 *
	 * @return the target search sequence node
	 */
	public JsonNode getSearchSequence() {
		return searchSequence;
	}
}
