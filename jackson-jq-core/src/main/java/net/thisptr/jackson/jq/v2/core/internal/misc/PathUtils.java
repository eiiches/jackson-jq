package net.thisptr.jackson.jq.v2.core.internal.misc;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class PathUtils {
	/**
	 * Returns whether the given path is an instance of {@link UntrackedPath} or {@link UnrepresentablePath}.
	 *
	 * @param path the path to check
	 * @return {@code true} if the path is lost or not tracked, {@code false} otherwise
	 */
	public static boolean isLost(Path<?> path) {
		return path instanceof UntrackedPath || path instanceof UnrepresentablePath;
	}

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
				JsonNode start = parseArraySliceIndices(jsonProvider, jsonProvider.requireGet(segObj, "start"));
				JsonNode end = parseArraySliceIndices(jsonProvider, jsonProvider.requireGet(segObj, "end"));
				path = path.appendIndexRange(jsonProvider, start, end);
			} else if (type == JsonNodeType.NUMBER) {
				path = path.appendIndex(jsonProvider, segObj);
			} else if (type == JsonNodeType.STRING) {
				path = path.appendKey(jsonProvider.asText(segObj));
			} else if (type == JsonNodeType.ARRAY) {
				path = path.appendIndexOf(jsonProvider, segObj);
			} else {
				path = path.appendInvalid(segObj);
			}
		}
		return path;
	}
}
