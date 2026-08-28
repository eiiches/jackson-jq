package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Represents an array index access step in a path (e.g. {@code .[0]}).
 * If the index is known to fit in an {@code int} at construction time, use {@link IntIndexPath} instead.
 *
 * @param <JsonNode> the JSON node type
 */
public final class NumberIndexPath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode index;

	/**
	 * Creates a {@code NumberIndexPath} with the given parent and numeric index node.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider used to validate the index node
	 * @param parent the parent path
	 * @param index the numeric index node
	 * @return a new {@code NumberIndexPath}
	 * @throws JsonQueryException if {@code index} is not a number
	 */
	static <JsonNode> NumberIndexPath<JsonNode> of(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> parent, JsonNode index) {
		if (jsonProvider.getNodeType(index) != JsonNodeType.NUMBER)
			throw new JsonQueryException("Array index must be a number");
		return new NumberIndexPath<>(parent, index);
	}

	/**
	 * Creates a new {@code NumberIndexPath}.
	 *
	 * @param parent the parent path
	 * @param index the numeric index node
	 */
	private NumberIndexPath(Path<JsonNode> parent, JsonNode index) {
		this.parent = parent;
		this.index = index;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		List<JsonNode> result = parent.toJsonList(jsonProvider);
		result.add(index);
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
	 * Returns the numeric index node.
	 *
	 * @return the numeric index node
	 */
	public JsonNode getIndex() {
		return index;
	}
}
