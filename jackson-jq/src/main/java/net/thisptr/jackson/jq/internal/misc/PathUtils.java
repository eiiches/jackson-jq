package net.thisptr.jackson.jq.internal.misc;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.ArrayIndexOfPath;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.InvalidPath;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class PathUtils {
	private static <JsonNode> JsonNode parseArraySliceIndices(final JsonProvider<JsonNode> jsonProvider, final JsonNode startOrEnd) throws JsonQueryException {
		if (startOrEnd == null)
			return jsonProvider.createNull();
		final JsonNodeType type = jsonProvider.getNodeType(startOrEnd);
		if (type == JsonNodeType.NUMBER)
			return startOrEnd;
		if (type == JsonNodeType.NULL)
			return startOrEnd;
		throw new JsonQueryException("Start and end indices of an array slice must be numbers");
	}

	public static <JsonNode> Path<JsonNode> toPath(final JsonProvider<JsonNode> jsonProvider, final JsonNode pathObj) throws JsonQueryException {
		if (jsonProvider.getNodeType(pathObj) != JsonNodeType.ARRAY)
			throw new JsonQueryException("Path must be specified as an array");
		Path<JsonNode> path = RootPath.getInstance();
		for (final JsonNode segObj : jsonProvider.iterate(pathObj)) {
			final JsonNodeType type = jsonProvider.getNodeType(segObj);
			if (type == JsonNodeType.OBJECT) {
				final JsonNode start = parseArraySliceIndices(jsonProvider, jsonProvider.get(segObj, "start"));
				final JsonNode end = parseArraySliceIndices(jsonProvider, jsonProvider.get(segObj, "end"));
				path = new ArrayRangeIndexPath<>(path, start, end);
			} else if (type == JsonNodeType.NUMBER) {
				path = new ArrayIndexPath<>(path, segObj);
			} else if (type == JsonNodeType.STRING) {
				path = new ObjectFieldPath<>(path, jsonProvider.asText(segObj));
			} else if (type == JsonNodeType.ARRAY) {
				path = new ArrayIndexOfPath<>(path, segObj);
			} else {
				path = new InvalidPath<>(path, segObj);
			}
		}
		return path;
	}
}
