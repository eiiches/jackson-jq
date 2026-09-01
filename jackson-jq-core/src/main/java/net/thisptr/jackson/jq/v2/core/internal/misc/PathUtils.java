package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.Iterator;

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

	/**
	 * Returns the {@code start} or {@code end} bound of an array slice path segment, e.g. the
	 * {@code {"start": 1, "end": 2}} in {@code getpath([{"start": 1, "end": 2}])}.
	 * <p>
	 * jq requires the field to be present; an explicit JSON {@code null} means an open bound, but a
	 * missing field is an error.
	 *
	 * @param jsonProvider the JSON provider
	 * @param sliceObj the object node describing the slice
	 * @param fieldName {@code "start"} or {@code "end"}
	 * @return the bound, either a number or JSON {@code null}
	 * @throws JsonQueryException if the field is missing or is neither a number nor JSON {@code null}
	 */
	public static <JsonNode> JsonNode getSliceBound(JsonProvider<JsonNode> jsonProvider, JsonNode sliceObj, String fieldName) throws JsonQueryException {
		JsonNode value = jsonProvider.getObjectField(sliceObj, fieldName);
		if (value == null)
			throw new JsonQueryException("Start and end indices of an array slice must be numbers");
		JsonNodeType type = jsonProvider.getNodeType(value);
		if (type != JsonNodeType.NUMBER && type != JsonNodeType.NULL)
			throw new JsonQueryException("Start and end indices of an array slice must be numbers");
		return value;
	}

	public static <JsonNode> Path<JsonNode> toPath(JsonProvider<JsonNode> jsonProvider, JsonNode pathObj) throws JsonQueryException {
		if (!jsonProvider.isArray(pathObj))
			throw new JsonQueryException("Path must be specified as an array");
		@Var Path<JsonNode> path = RootPath.getInstance();
		for (Iterator<JsonNode> it = jsonProvider.getArrayElements(pathObj); it.hasNext(); ) {
			JsonNode segObj = it.next();
			JsonNodeType type = jsonProvider.getNodeType(segObj);
			if (type == JsonNodeType.OBJECT) {
				JsonNode start = getSliceBound(jsonProvider, segObj, "start");
				JsonNode end = getSliceBound(jsonProvider, segObj, "end");
				path = path.appendIndexRange(jsonProvider, start, end);
			} else if (type == JsonNodeType.NUMBER) {
				path = path.appendIndex(jsonProvider, segObj);
			} else if (type == JsonNodeType.STRING) {
				path = path.appendKey(jsonProvider.getString(segObj));
			} else if (type == JsonNodeType.ARRAY) {
				path = path.appendIndexOf(jsonProvider, segObj);
			} else {
				path = path.appendInvalid(segObj);
			}
		}
		return path;
	}
}
