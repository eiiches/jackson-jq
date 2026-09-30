package net.thisptr.jackson.jq.v2.core.internal.path.utils;

import java.util.Iterator;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class PathUtils {
	/**
	 * Marks a path emitted under path tracking as stale at the position the caller was at.
	 * <p>
	 * An expression that cannot produce a path emits {@link UntrackedPath} regardless of what it was
	 * given, so a caller that was tracking has to record where the traversal had got to. Anything else,
	 * including an already stale path, is returned unchanged.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param emitted the path that was emitted
	 * @param ipath the path the caller was at
	 * @param in the value at {@code ipath}
	 * @return {@code emitted}, or a stale path at {@code ipath}
	 */
	public static <JsonNode> Path<JsonNode> stale(Path<JsonNode> emitted, Path<JsonNode> ipath, JsonNode in) {
		if (!(emitted instanceof UntrackedPath) || ipath instanceof UntrackedPath)
			return emitted;
		return UnrepresentablePath.of(ipath, in);
	}

	/**
	 * Resumes a stale path when {@code value} is still the value recorded at the position it went stale
	 * at, and returns {@code null} when the path is really lost.
	 * <p>
	 * Any other path is returned unchanged, {@link UntrackedPath} included: that one means the caller
	 * was never tracking a path, so there is nothing to resume and nothing to reject.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider
	 * @param version the jq compatibility version
	 * @param path the path to resume
	 * @param value the value being looked at
	 * @return the resumed path, or {@code null} if the path is lost
	 */
	public static <JsonNode> @Nullable Path<JsonNode> recover(JsonProvider<JsonNode> jsonProvider, Version version, Path<JsonNode> path, JsonNode value) {
		if (!(path instanceof UnrepresentablePath<JsonNode> stalePath))
			return path;
		if (!isSameValue(jsonProvider, version, stalePath.getValueAtLastValidPath(), value))
			return null;
		return stalePath.getLastValidPath();
	}

	/**
	 * Returns the path a path expression produced for {@code value}, resuming a stale one where jq
	 * would, and reporting jq's invalid path expression error where it would not.
	 * <p>
	 * This is what {@code path/1} and the assignment operators do with each value their argument emits:
	 * the argument is evaluated from {@link RootPath}, so a value that arrives with no path at all went
	 * stale right there, at the root, where {@code in} was.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider
	 * @param version the jq compatibility version
	 * @param path the path the value was emitted with
	 * @param in the input the path expression was evaluated against
	 * @param value the emitted value
	 * @return the path of {@code value}
	 * @throws JsonQueryException if {@code value} has no path
	 */
	public static <JsonNode> Path<JsonNode> requirePath(JsonProvider<JsonNode> jsonProvider, Version version, Path<JsonNode> path, JsonNode in, JsonNode value) throws JsonQueryException {
		Path<JsonNode> recovered = recover(jsonProvider, version, stale(path, RootPath.getInstance(), in), value);
		if (recovered == null)
			throw new JsonQueryException(String.format("Invalid path expression with result %s",
					ExceptionMessages.truncate(JsonNodeUtils.toString(jsonProvider, value, version), version, ExceptionMessages.LONG_BUFFER_SIZE)));
		return recovered;
	}

	/**
	 * Returns whether jq would consider the two values one and the same, which is how it decides that a
	 * traversal is still at the position it recorded.
	 * <p>
	 * jq compares the two by identity rather than by value: the same value passed along, through a
	 * variable or a pipe, is one jq recognises. Beyond that only the kinds it stores inline in a value
	 * can compare equal without being the very same value: {@code null} and the booleans on every
	 * version, and numbers below jq 1.7, where a number was still a bare {@code double}. A string, an
	 * array or an object built by an expression is a fresh allocation and never matches. The comparison
	 * for numbers is over the raw bits, so {@code 0} and {@code -0} differ.
	 * <p>
	 * This is an artifact of how jq represents values rather than a documented rule, and the set of
	 * kinds it accepts moved when jq 1.7 gave numbers a literal representation. It is reproduced here
	 * because every supported jq behaves this way.
	 */
	private static <JsonNode> boolean isSameValue(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode a, JsonNode b) {
		if (a == b)
			return true;
		JsonNodeType type = jsonProvider.getNodeType(a);
		if (type != jsonProvider.getNodeType(b))
			return false;
		switch (type) {
			case NULL:
				return true;
			case BOOLEAN:
				return jsonProvider.getBoolean(a) == jsonProvider.getBoolean(b);
			case NUMBER:
				return version.compareTo(Versions.JQ_1_7) < 0
						&& Double.doubleToRawLongBits(jsonProvider.getNumberAsDoubleRounded(a)) == Double.doubleToRawLongBits(jsonProvider.getNumberAsDoubleRounded(b));
			default:
				return false;
		}
	}

	/**
	 * Returns the {@code start} or {@code end} bound of an array slice path segment, e.g. the
	 * {@code {"start": 1, "end": 2}} in {@code getpath([{"start": 1, "end": 2}])}.
	 * <p>
	 * jq requires the member to be present; an explicit JSON {@code null} means an open bound, but a
	 * missing member is an error.
	 *
	 * @param jsonProvider the JSON provider
	 * @param sliceObj the object node describing the slice
	 * @param name {@code "start"} or {@code "end"}
	 * @return the bound
	 * @throws JsonQueryException if the member is missing
	 */
	public static <JsonNode> JsonNode getSliceBound(JsonProvider<JsonNode> jsonProvider, JsonNode sliceObj, String name) throws JsonQueryException {
		Maybe<JsonNode> value = jsonProvider.getObjectMember(sliceObj, name);
		if (value.isAbsent())
			throw new JsonQueryException("Start and end indices of an array slice must be numbers");
		return value.get();
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
