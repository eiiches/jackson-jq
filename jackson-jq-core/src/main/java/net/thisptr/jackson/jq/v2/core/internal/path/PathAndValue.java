package net.thisptr.jackson.jq.v2.core.internal.path;

import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A value together with the path it was reached by.
 * <p>
 * {@code value} is not annotated {@code @Nullable} on purpose: a provider whose underlying library
 * represents JSON {@code null} as Java {@code null} stores that {@code null} here as a value, and it
 * must never be read as "there is nothing here".
 */
public class PathAndValue<JsonNode> {
	private final Path<JsonNode> path;
	private final JsonNode value;

	public PathAndValue(Path<JsonNode> path, JsonNode value) {
		this.value = value;
		this.path = path;
	}

	public Path<JsonNode> getPath() {
		return path;
	}

	public JsonNode getValue() {
		return value;
	}
}
