package net.thisptr.jackson.jq.v2.core.internal.utils;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.path.Path;

public class PathAndValue<JsonNode> {
	private final Path<JsonNode> path;
	private final @Nullable JsonNode value;

	public PathAndValue(Path<JsonNode> path, @Nullable JsonNode value) {
		this.value = value;
		this.path = path;
	}

	public Path<JsonNode> getPath() {
		return path;
	}

	public @Nullable JsonNode getValue() {
		return value;
	}
}
