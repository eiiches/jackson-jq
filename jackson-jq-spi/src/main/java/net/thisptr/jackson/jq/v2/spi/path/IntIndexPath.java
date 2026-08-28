package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Represents an array index access step in a path (e.g. {@code .[0]}).
 *
 * @param <JsonNode> the JSON node type
 */
public final class IntIndexPath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final int index;

	/**
	 * Creates an {@code IntIndexPath} with the given parent and integer index.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param parent the parent path
	 * @param index the array index
	 * @return a new {@code IntIndexPath}
	 */
	static <JsonNode> IntIndexPath<JsonNode> of(Path<JsonNode> parent, int index) {
		return new IntIndexPath<>(parent, index);
	}

	/**
	 * Creates a new {@code IntIndexPath}.
	 *
	 * @param parent the parent path
	 * @param index the array index
	 */
	private IntIndexPath(Path<JsonNode> parent, int index) {
		this.parent = parent;
		this.index = index;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		List<JsonNode> result = parent.toJsonList(jsonProvider);
		result.add(jsonProvider.createNumber(index));
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
	 * Returns the array index.
	 *
	 * @return the array index
	 */
	public int getIndex() {
		return index;
	}
}
