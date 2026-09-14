package net.thisptr.jackson.jq.v2.spi.path;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Represents a slice / range index step in a path (e.g. {@code .[1:3]}).
 *
 * @param <JsonNode> the JSON node type
 */
public final class IndexRangePath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final JsonNode startIndex;
	private final JsonNode endIndex;

	/**
	 * Creates an {@code IndexRangePath} with the given parent and start/end range bounds.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param parent the parent path
	 * @param start the start index node
	 * @param end the end index node
	 * @return a new {@code IndexRangePath}
	 */
	static <JsonNode> IndexRangePath<JsonNode> of(Path<JsonNode> parent, JsonNode start, JsonNode end) {
		return new IndexRangePath<>(parent, start, end);
	}

	/**
	 * Creates a new {@code IndexRangePath}.
	 *
	 * @param parent the parent path
	 * @param startIndex the start index node
	 * @param endIndex the end index node
	 */
	private IndexRangePath(Path<JsonNode> parent, JsonNode startIndex, JsonNode endIndex) {
		this.parent = parent;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		List<JsonNode> result = parent.toJsonList(jsonProvider);
		Map<String, JsonNode> segment = new LinkedHashMap<>();
		segment.put("start", startIndex);
		segment.put("end", endIndex);
		result.add(jsonProvider.createObject(segment));
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
	 * Returns the start index node.
	 *
	 * @return the start index node
	 */
	public JsonNode getStartIndex() {
		return startIndex;
	}

	/**
	 * Returns the end index node.
	 *
	 * @return the end index node
	 */
	public JsonNode getEndIndex() {
		return endIndex;
	}
}
