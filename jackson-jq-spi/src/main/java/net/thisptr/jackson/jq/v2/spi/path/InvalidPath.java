package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Represents an invalid path step (e.g. indexing into a non-array or non-object value).
 *
 * @param <JsonNode> the JSON node type
 */
public final class InvalidPath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode index;

	/**
	 * Creates an {@code InvalidPath} with the given parent and invalid index or field node.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param parent the parent path
	 * @param index the invalid index or field node
	 * @return a new {@code InvalidPath}
	 */
	static <JsonNode> InvalidPath<JsonNode> of(Path<JsonNode> parent, JsonNode index) {
		return new InvalidPath<>(parent, index);
	}

	/**
	 * Creates a new {@code InvalidPath}.
	 *
	 * @param parent the parent path
	 * @param index the invalid index or field node
	 */
	private InvalidPath(Path<JsonNode> parent, JsonNode index) {
		this.parent = parent;
		this.index = index;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		throw new JsonQueryException("Invalid path expression");
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
	 * Returns the invalid index or field node.
	 *
	 * @return the invalid index or field node
	 */
	public JsonNode getIndex() {
		return index;
	}
}
