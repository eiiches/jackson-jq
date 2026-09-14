package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Represents an object field access step in a path (e.g. {@code .foo} or {@code .["foo"]}).
 *
 * @param <JsonNode> the JSON node type
 */
public final class StringKeyPath<JsonNode> extends Path<JsonNode> {
	private final Path<JsonNode> parent;
	private final String key;

	/**
	 * Creates a {@code StringKeyPath} with the given parent and field key.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param parent the parent path
	 * @param key the field key
	 * @return a new {@code StringKeyPath}
	 */
	static <JsonNode> StringKeyPath<JsonNode> of(Path<JsonNode> parent, String key) {
		return new StringKeyPath<>(parent, key);
	}

	/**
	 * Creates a new {@code StringKeyPath}.
	 *
	 * @param parent the parent path
	 * @param key the field key
	 */
	private StringKeyPath(Path<JsonNode> parent, String key) {
		this.parent = parent;
		this.key = key;
	}

	@Override
	public List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider) {
		List<JsonNode> result = parent.toJsonList(jsonProvider);
		result.add(jsonProvider.createString(key));
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
	 * Returns the field key.
	 *
	 * @return the field key
	 */
	public String getKey() {
		return key;
	}
}
