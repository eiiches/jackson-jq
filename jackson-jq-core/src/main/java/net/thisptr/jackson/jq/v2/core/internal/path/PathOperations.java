package net.thisptr.jackson.jq.v2.core.internal.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.Range;
import net.thisptr.jackson.jq.v2.core.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.IndexOfPath;
import net.thisptr.jackson.jq.v2.spi.path.IndexRangePath;
import net.thisptr.jackson.jq.v2.spi.path.IntIndexPath;
import net.thisptr.jackson.jq.v2.spi.path.InvalidPath;
import net.thisptr.jackson.jq.v2.spi.path.NumberIndexPath;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.StringKeyPath;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;

/**
 * Core operations over the data-only {@link Path} hierarchy.
 */
public final class PathOperations {
	@FunctionalInterface
	public interface Mutation<JsonNode> {
		/**
		 * Computes the replacement for the node currently at a path location.
		 *
		 * @return the new value; deletion is not expressed through {@link #mutate}, see
		 * {@code DelPathsFunction}
		 */
		JsonNode apply(@Nullable JsonNode node) throws JsonQueryException;
	}

	private PathOperations() {
	}

	// The casts are safe because every concrete path retains the JsonNode type of its parent and component.
	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> path, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive, Version version) throws JsonQueryException {
		resolveUnchecked(jsonProvider, path, in, ipath, output, permissive, version);
	}

	// The casts are safe because every concrete path retains the JsonNode type of its parent and component.
	private static <JsonNode> void resolveUnchecked(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> path, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive, Version version) throws JsonQueryException {
		if (path instanceof RootPath<?>) {
			output.emit(in, ipath);
			return;
		}
		if (path instanceof UnrepresentablePath<?>)
			throw new JsonQueryException("Invalid path expression");
		if (path instanceof StringKeyPath<?>) {
			StringKeyPath<JsonNode> objectPath = (StringKeyPath<JsonNode>) path;
			resolveUnchecked(jsonProvider, objectPath.getParentPath(), in, ipath, (parent, parentPath) -> {
				resolveObjectField(jsonProvider, parent, parentPath, output, objectPath.getKey(), permissive, version);
			}, permissive, version);
			return;
		}
		if (path instanceof NumberIndexPath<?>) {
			NumberIndexPath<JsonNode> indexPath = (NumberIndexPath<JsonNode>) path;
			resolveUnchecked(jsonProvider, indexPath.getParentPath(), in, ipath, (parent, parentPath) -> {
				resolveArrayIndex(jsonProvider, parent, parentPath, output, indexPath.getIndex(), permissive, version);
			}, permissive, version);
			return;
		}
		if (path instanceof IntIndexPath<?>) {
			IntIndexPath<JsonNode> indexPath = (IntIndexPath<JsonNode>) path;
			resolveUnchecked(jsonProvider, indexPath.getParentPath(), in, ipath, (parent, parentPath) -> {
				resolveArrayIndex(jsonProvider, parent, parentPath, output, indexPath.getIndex(), permissive, version);
			}, permissive, version);
			return;
		}
		if (path instanceof IndexRangePath<?>) {
			IndexRangePath<JsonNode> rangePath = (IndexRangePath<JsonNode>) path;
			resolveUnchecked(jsonProvider, rangePath.getParentPath(), in, ipath, (parent, parentPath) -> {
				resolveArrayRangeIndex(jsonProvider, parent, parentPath, output, rangePath.getStartIndex(), rangePath.getEndIndex(), permissive, version);
			}, permissive, version);
			return;
		}
		if (path instanceof IndexOfPath<?>) {
			IndexOfPath<JsonNode> indexOfPath = (IndexOfPath<JsonNode>) path;
			resolveUnchecked(jsonProvider, indexOfPath.getParentPath(), in, ipath, (parent, parentPath) -> {
				resolveArrayIndexOf(jsonProvider, parent, parentPath, output, indexOfPath.getSearchSequence(), permissive, version);
			}, permissive, version);
			return;
		}
		if (path instanceof InvalidPath<?>) {
			InvalidPath<JsonNode> invalidPath = (InvalidPath<JsonNode>) path;
			resolveUnchecked(jsonProvider, invalidPath.getParentPath(), in, ipath, (parent, parentPath) -> {
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, invalidPath.getIndex()));
			}, permissive, version);
			return;
		}
		throw unsupported(path);
	}

	public static <JsonNode> void resolveObjectField(JsonProvider<JsonNode> jsonProvider, JsonNode parent, Path<JsonNode> parentPath, Output<JsonNode> output, String key, boolean permissive, Version version) throws JsonQueryException {
		if (jsonProvider.isNull(parent)) {
			output.emit(jsonProvider.createNull(), parentPath.appendKey(key));
		} else if (jsonProvider.isObject(parent)) {
			JsonNode node = jsonProvider.getObjectField(parent, key);
			output.emit(node == null ? jsonProvider.createNull() : node, parentPath.appendKey(key));
		} else if (!permissive) {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, parent, jsonProvider.createString(key)));
		}
	}

	public static <JsonNode> void resolveArrayIndex(JsonProvider<JsonNode> jsonProvider, JsonNode parent, Path<JsonNode> parentPath, Output<JsonNode> output, JsonNode index, boolean permissive, Version version) throws JsonQueryException {
		assert jsonProvider.isNumber(index);
		if (jsonProvider.isArray(parent)) {
			double indexAsDouble = jsonProvider.getNumberAsDoubleRounded(index);
			// NaN, the infinities and anything outside int range all address nothing.
			Integer truncated = jsonProvider.getNumberAsIntTruncated(index);
			if (truncated == null) {
				output.emit(jsonProvider.createNull(), parentPath.appendIndex(jsonProvider, index));
				return;
			}
			int indexAsInt = truncated;
			if (version.compareTo(Versions.JQ_1_7) < 0 && indexAsDouble != indexAsInt) {
				output.emit(jsonProvider.createNull(), parentPath.appendIndex(jsonProvider, index));
				return;
			}
			int resolvedIndex = indexAsInt < 0 ? indexAsInt + jsonProvider.getArrayLength(parent) : indexAsInt;
			if (resolvedIndex < 0 || jsonProvider.getArrayLength(parent) <= resolvedIndex) {
				output.emit(jsonProvider.createNull(), parentPath.appendIndex(jsonProvider, index));
				return;
			}
			output.emit(jsonProvider.getArrayElement(parent, resolvedIndex), parentPath.appendIndex(jsonProvider, index));
		} else if (jsonProvider.isNull(parent)) {
			output.emit(jsonProvider.createNull(), parentPath.appendIndex(jsonProvider, index));
		} else if (!permissive) {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, parent, index));
		}
	}

	public static <JsonNode> void resolveArrayIndex(JsonProvider<JsonNode> jsonProvider, JsonNode parent, Path<JsonNode> parentPath, Output<JsonNode> output, int index, boolean permissive, Version version) throws JsonQueryException {
		if (jsonProvider.isArray(parent)) {
			int resolvedIndex = index < 0 ? index + jsonProvider.getArrayLength(parent) : index;
			if (resolvedIndex < 0 || jsonProvider.getArrayLength(parent) <= resolvedIndex) {
				output.emit(jsonProvider.createNull(), parentPath.appendIndex(index));
				return;
			}
			output.emit(jsonProvider.getArrayElement(parent, resolvedIndex), parentPath.appendIndex(index));
		} else if (jsonProvider.isNull(parent)) {
			output.emit(jsonProvider.createNull(), parentPath.appendIndex(index));
		} else if (!permissive) {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, parent, jsonProvider.createNumber(index)));
		}
	}

	public static <JsonNode> void resolveArrayRangeIndex(JsonProvider<JsonNode> jsonProvider, JsonNode parent, Path<JsonNode> parentPath, Output<JsonNode> output, JsonNode start, JsonNode end, boolean permissive, Version version) throws JsonQueryException {
		assert jsonProvider.isNull(start) || jsonProvider.isNumber(start);
		assert jsonProvider.isNull(end) || jsonProvider.isNumber(end);
		if (jsonProvider.isArray(parent)) {
			Range range = Range.resolve(jsonProvider, start, end, jsonProvider.getArrayLength(parent));
			List<JsonNode> subarray = new ArrayList<>((int) (range.end - range.start));
			for (long index = range.start; index < range.end; ++index)
				subarray.add(jsonProvider.getArrayElement(parent, (int) index));
			output.emit(jsonProvider.createArray(subarray), parentPath.appendIndexRange(jsonProvider, start, end));
		} else if (jsonProvider.isString(parent)) {
			Range range = Range.resolve(jsonProvider, start, end, UnicodeUtils.lengthUtf32(jsonProvider.getString(parent)));
			JsonNode substring = jsonProvider.createString(UnicodeUtils.substringUtf32(jsonProvider.getString(parent), (int) range.start, (int) range.end));
			output.emit(substring, parentPath.appendIndexRange(jsonProvider, start, end));
		} else if (jsonProvider.isNull(parent)) {
			output.emit(jsonProvider.createNull(), parentPath.appendIndexRange(jsonProvider, start, end));
		} else if (!permissive) {
			Map<String, JsonNode> subpath = new LinkedHashMap<>();
			subpath.put("start", start);
			subpath.put("end", end);
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, parent, jsonProvider.createObject(subpath)));
		}
	}

	public static <JsonNode> void resolveArrayIndexOf(JsonProvider<JsonNode> jsonProvider, JsonNode parent, Path<JsonNode> parentPath, Output<JsonNode> output, JsonNode subsequence, boolean permissive, Version version) throws JsonQueryException {
		assert jsonProvider.isArray(subsequence);
		if (jsonProvider.isArray(parent)) {
			JsonNode indexList = indexOfAll(jsonProvider, parent, subsequence);
			output.emit(indexList, parentPath.appendIndexOf(jsonProvider, subsequence));
		} else if (!permissive) {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, parent, subsequence));
		}
	}

	public static <JsonNode> JsonNode mutate(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> path, JsonNode in, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		return mutateUnchecked(jsonProvider, path, in, mutation, version);
	}

	// The casts are safe because every concrete path retains the JsonNode type of its parent and component.
	private static <JsonNode> JsonNode mutateUnchecked(JsonProvider<JsonNode> jsonProvider, Path<JsonNode> path, JsonNode in, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		if (path instanceof RootPath<?>)
			return mutation.apply(in);
		if (path instanceof UnrepresentablePath<?>)
			throw new JsonQueryException("Invalid path expression");
		if (path instanceof StringKeyPath<?>) {
			StringKeyPath<JsonNode> objectPath = (StringKeyPath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, objectPath.getParentPath(), in, oldValue -> {
				return mutateObjectField(jsonProvider, oldValue, objectPath.getKey(), mutation, version);
			}, version);
		}
		if (path instanceof NumberIndexPath<?>) {
			NumberIndexPath<JsonNode> indexPath = (NumberIndexPath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, indexPath.getParentPath(), in, oldValue -> {
				return mutateArrayIndex(jsonProvider, oldValue, indexPath.getIndex(), mutation, version);
			}, version);
		}
		if (path instanceof IntIndexPath<?>) {
			IntIndexPath<JsonNode> indexPath = (IntIndexPath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, indexPath.getParentPath(), in, oldValue -> {
				return mutateArrayIndex(jsonProvider, oldValue, indexPath.getIndex(), mutation, version);
			}, version);
		}
		if (path instanceof IndexRangePath<?>) {
			IndexRangePath<JsonNode> rangePath = (IndexRangePath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, rangePath.getParentPath(), in, oldValue -> {
				return mutateArrayRangeIndex(jsonProvider, oldValue, rangePath.getStartIndex(), rangePath.getEndIndex(), mutation, version);
			}, version);
		}
		if (path instanceof IndexOfPath<?>) {
			IndexOfPath<JsonNode> indexOfPath = (IndexOfPath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, indexOfPath.getParentPath(), in, oldValue -> {
				throw new JsonQueryException("Cannot update field at array index of array");
			}, version);
		}
		if (path instanceof InvalidPath<?>) {
			InvalidPath<JsonNode> invalidPath = (InvalidPath<JsonNode>) path;
			return mutateUnchecked(jsonProvider, invalidPath.getParentPath(), in, oldValue -> {
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, invalidPath.getIndex()));
			}, version);
		}
		throw unsupported(path);
	}

	private static <JsonNode> JsonNode mutateObjectField(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, String key, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		if (in == null || jsonProvider.isNull(in))
			in = jsonProvider.createObject(Collections.emptyMap());
		if (jsonProvider.isObject(in)) {
			Map<String, JsonNode> values = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> iterator = jsonProvider.getObjectEntries(in);
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> entry = iterator.next();
				values.put(entry.getKey(), entry.getValue());
			}
			JsonNode newValue = mutation.apply(values.get(key));
			values.put(key, newValue);
			return jsonProvider.createObject(values);
		}
		throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createString(key)));
	}

	private static <JsonNode> JsonNode mutateArrayIndex(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, JsonNode index, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		assert jsonProvider.isNumber(index);
		if (in == null || jsonProvider.isNull(in))
			in = jsonProvider.createArray(Collections.emptyList());
		if (jsonProvider.isArray(in)) {
			double indexAsDouble = jsonProvider.getNumberAsDoubleRounded(index);
			if (Double.isNaN(indexAsDouble) || Double.isInfinite(indexAsDouble))
				throw new JsonQueryException("Cannot use " + (Double.isNaN(indexAsDouble) ? "nan" : "infinite") + " as array index");
			Integer truncated = jsonProvider.getNumberAsIntTruncated(index);
			if (truncated == null)
				throw new JsonQueryException("Array index too large");
			int indexAsInt = truncated;
			int resolvedIndex = indexAsInt < 0 ? indexAsInt + jsonProvider.getArrayLength(in) : indexAsInt;
			if (resolvedIndex < 0)
				throw new JsonQueryException("Out of bounds negative array index");

			JsonNode newValue = mutation.apply(resolvedIndex < jsonProvider.getArrayLength(in) ? jsonProvider.getArrayElement(in, resolvedIndex) : null);

			List<JsonNode> out = new ArrayList<>(Math.max(jsonProvider.getArrayLength(in), resolvedIndex + 1));
			for (int i = 0; i < jsonProvider.getArrayLength(in); ++i)
				out.add(jsonProvider.getArrayElement(in, i));
			for (int i = jsonProvider.getArrayLength(in); i <= resolvedIndex; ++i)
				out.add(jsonProvider.createNull());
			out.set(resolvedIndex, newValue);
			return jsonProvider.createArray(out);
		}
		throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, index));
	}

	private static <JsonNode> JsonNode mutateArrayIndex(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, int index, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		if (in == null || jsonProvider.isNull(in))
			in = jsonProvider.createArray(Collections.emptyList());
		if (jsonProvider.isArray(in)) {
			int resolvedIndex = index < 0 ? index + jsonProvider.getArrayLength(in) : index;
			if (resolvedIndex < 0)
				throw new JsonQueryException("Out of bounds negative array index");

			JsonNode newValue = mutation.apply(resolvedIndex < jsonProvider.getArrayLength(in) ? jsonProvider.getArrayElement(in, resolvedIndex) : null);

			List<JsonNode> out = new ArrayList<>(Math.max(jsonProvider.getArrayLength(in), resolvedIndex + 1));
			for (int i = 0; i < jsonProvider.getArrayLength(in); ++i)
				out.add(jsonProvider.getArrayElement(in, i));
			for (int i = jsonProvider.getArrayLength(in); i <= resolvedIndex; ++i)
				out.add(jsonProvider.createNull());
			out.set(resolvedIndex, newValue);
			return jsonProvider.createArray(out);
		}
		throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(index)));
	}

	private static <JsonNode> JsonNode mutateArrayRangeIndex(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, JsonNode start, JsonNode end, Mutation<JsonNode> mutation, Version version) throws JsonQueryException {
		assert jsonProvider.isNull(start) || jsonProvider.isNumber(start);
		assert jsonProvider.isNull(end) || jsonProvider.isNumber(end);
		if (in == null)
			in = jsonProvider.createNull();
		if (jsonProvider.isArray(in)) {
			Range range = Range.resolve(jsonProvider, start, end, jsonProvider.getArrayLength(in));

			List<JsonNode> oldSlice = new ArrayList<>((int) (range.end - range.start));
			for (long index = range.start; index < range.end; ++index)
				oldSlice.add(jsonProvider.getArrayElement(in, (int) index));
			JsonNode newValue = mutation.apply(jsonProvider.createArray(oldSlice));
			if (!jsonProvider.isArray(newValue))
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");

			List<JsonNode> out = new ArrayList<>((int) range.start + jsonProvider.getArrayLength(newValue) + (jsonProvider.getArrayLength(in) - (int) range.end));
			for (int index = 0; index < range.start; ++index)
				out.add(jsonProvider.getArrayElement(in, index));
			Iterator<JsonNode> iterator = jsonProvider.getArrayElements(newValue);
			while (iterator.hasNext())
				out.add(iterator.next());
			for (long index = range.end; index < jsonProvider.getArrayLength(in); ++index)
				out.add(jsonProvider.getArrayElement(in, (int) index));
			return jsonProvider.createArray(out);
		}
		if (jsonProvider.isString(in))
			throw new JsonQueryException("Cannot update field at object index of string");
		if (jsonProvider.isNull(in)) {
			JsonNode newValue = mutation.apply(jsonProvider.createNull());
			if (!jsonProvider.isArray(newValue))
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");
			return newValue;
		}
		Map<String, JsonNode> subpath = new LinkedHashMap<>();
		subpath.put("start", start);
		subpath.put("end", end);
		throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createObject(subpath)));
	}

	private static <JsonNode> JsonNode indexOfAll(JsonProvider<JsonNode> jsonProvider, JsonNode sequence, JsonNode subsequence) {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		List<JsonNode> out = new ArrayList<>();
		if (jsonProvider.getArrayLength(subsequence) != 0) {
			shift:
			for (int i = 0; i < jsonProvider.getArrayLength(sequence) - jsonProvider.getArrayLength(subsequence) + 1; ++i) {
				for (int j = 0; j < jsonProvider.getArrayLength(subsequence); ++j)
					if (comparator.compare(jsonProvider.getArrayElement(sequence, i + j), jsonProvider.getArrayElement(subsequence, j)) != 0)
						continue shift;
				out.add(jsonProvider.createNumber(i));
			}
		}
		return jsonProvider.createArray(out);
	}

	private static JsonQueryException unsupported(Path<?> path) {
		return new JsonQueryException("Unsupported path implementation: " + path.getClass().getName());
	}
}
