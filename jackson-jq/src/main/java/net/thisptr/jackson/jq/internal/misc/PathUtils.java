package net.thisptr.jackson.jq.internal.misc;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.InvalidPath;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class PathUtils {
	private static Long parseArraySliceIndices(final JsonNode startOrEnd) throws JsonQueryException {
		if (startOrEnd == null)
			return null;
		if (startOrEnd.isNull())
			return null;
		if (startOrEnd.isIntegralNumber())
			return startOrEnd.asLong();
		throw new JsonQueryException("Start and end indices of an array slice must be numbers");
	}

	public static Path toPath(final JsonNode pathObj) throws JsonQueryException {
		if (!pathObj.isArray())
			throw new JsonQueryException("Path must be specified as an array");
		Path path = RootPath.getInstance();
		for (final JsonNode segObj : pathObj) {
			if (segObj.isObject()) {
				final Long start = parseArraySliceIndices(segObj.get("start"));
				final Long end = parseArraySliceIndices(segObj.get("end"));
				path = new ArrayRangeIndexPath(path, start, end);
			} else if (segObj.isIntegralNumber()) {
				path = new ArrayIndexPath(path, segObj.asInt());
			} else if (segObj.isTextual()) {
				path = new ObjectFieldPath(path, segObj.asText());
			} else {
				path = new InvalidPath(path, segObj);
			}
		}
		return path;
	}
}
