package net.thisptr.jackson.jq.v2.core.internal.misc;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.path.ArrayIndexOfPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.v2.core.path.InvalidPath;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class PathUtils {
	private static <JsonNode> JsonNode parseArraySliceIndices(JsonProvider<JsonNode> jsonProvider, JsonNode startOrEnd) throws JsonQueryException {
		if (startOrEnd == null)
			return jsonProvider.createNull();
		JsonNodeType type = jsonProvider.getNodeType(startOrEnd);
		if (type == JsonNodeType.NUMBER)
			return startOrEnd;
		if (type == JsonNodeType.NULL)
			return startOrEnd;
		throw new JsonQueryException("Start and end indices of an array slice must be numbers");
	}

	public static <JsonNode> Path<JsonNode> toPath(JsonProvider<JsonNode> jsonProvider, JsonNode pathObj) throws JsonQueryException {
		if (jsonProvider.getNodeType(pathObj) != JsonNodeType.ARRAY)
			throw new JsonQueryException("Path must be specified as an array");
		@Var Path<JsonNode> path = RootPath.getInstance();
		for (JsonNode segObj : jsonProvider.iterate(pathObj)) {
			JsonNodeType type = jsonProvider.getNodeType(segObj);
			if (type == JsonNodeType.OBJECT) {
				JsonNode start = parseArraySliceIndices(jsonProvider, jsonProvider.get(segObj, "start"));
				JsonNode end = parseArraySliceIndices(jsonProvider, jsonProvider.get(segObj, "end"));
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
