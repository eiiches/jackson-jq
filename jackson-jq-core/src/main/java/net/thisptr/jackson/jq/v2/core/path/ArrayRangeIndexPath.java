package net.thisptr.jackson.jq.v2.core.path;

import java.util.Iterator;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Range;
import net.thisptr.jackson.jq.v2.core.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Despite the name, {@link ArrayRangeIndexPath} can be used to index strings.
 */
public class ArrayRangeIndexPath<JsonNode> implements Path<JsonNode> {
	public final JsonNode start;
	public final JsonNode end;
	private final Path<JsonNode> parent;
	private final @Nullable Version version;

	public static <JsonNode> @Nullable ArrayRangeIndexPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode start, JsonNode end) {
		return chainIfNotNull(parent, start, end, null);
	}

	public static <JsonNode> @Nullable ArrayRangeIndexPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode start, JsonNode end, @Nullable Version version) {
		if (parent == null)
			return null;
		return new ArrayRangeIndexPath<>(parent, start, end, version);
	}

	public ArrayRangeIndexPath(Path<JsonNode> parent, JsonNode start, JsonNode end) {
		this(parent, start, end, null);
	}

	public ArrayRangeIndexPath(Path<JsonNode> parent, JsonNode start, JsonNode end, @Nullable Version version) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		if (start == null)
			throw new NullPointerException("start must not be null");
		if (end == null)
			throw new NullPointerException("end must not be null");
		// Note: cannot validate isNumber()/isNull() without JsonProvider here
		this.parent = parent;
		this.start = start;
		this.end = end;
		this.version = version;
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, start, end, mutation, version);
		}, makeParent);
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		JsonNode range = jsonProvider.createObject();
		jsonProvider.set(range, "start", start);
		jsonProvider.set(range, "end", end);
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, range);
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, start, end, permissive, version);
		}, permissive);
	}

	private static <JsonNode> JsonNode mutate(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, JsonNode start, JsonNode end, Mutation<JsonNode> mutation, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(start) == JsonNodeType.NULL || jsonProvider.getNodeType(start) == JsonNodeType.NUMBER;
		assert jsonProvider.getNodeType(end) == JsonNodeType.NULL || jsonProvider.getNodeType(end) == JsonNodeType.NUMBER;
		if (in == null)
			in = jsonProvider.createNull();
		if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			Range r = Range.resolve(jsonProvider, start, end, jsonProvider.size(in));
			JsonNode out = jsonProvider.createArray();
			for (int index = 0; index < r.start; ++index)
				jsonProvider.add(out, jsonProvider.requireGet(in, index));

			JsonNode oldval = jsonProvider.createArray();
			for (long index = r.start; index < r.end; ++index)
				jsonProvider.add(oldval, jsonProvider.requireGet(in, (int) index));
			JsonNode newval = mutation.apply(oldval);
			if (newval == null)
				throw new JsonQueryTypeException("A slice of an array cannot be deleted");
			if (jsonProvider.getNodeType(newval) != JsonNodeType.ARRAY)
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");
			Iterator<JsonNode> iter = jsonProvider.elements(newval);
			while (iter.hasNext())
				jsonProvider.add(out, iter.next());
			for (long index = r.end; index < jsonProvider.size(in); ++index)
				jsonProvider.add(out, jsonProvider.requireGet(in, (int) index));

			return out;
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.STRING) {
			throw new JsonQueryException("Cannot update field at object index of string");
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			JsonNode newval = mutation.apply(jsonProvider.createNull());
			if (newval == null)
				throw new JsonQueryTypeException("A slice of an array cannot be deleted");
			if (jsonProvider.getNodeType(newval) != JsonNodeType.ARRAY)
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");
			return newval;
		} else {
			JsonNode subpath = jsonProvider.createObject();
			jsonProvider.set(subpath, "start", start);
			jsonProvider.set(subpath, "end", end);
			throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, subpath));
		}
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, JsonNode start, JsonNode end, boolean permissive) throws JsonQueryException {
		resolve(jsonProvider, pobj, ppath, output, start, end, permissive, null);
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, JsonNode start, JsonNode end, boolean permissive, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(start) == JsonNodeType.NULL || jsonProvider.getNodeType(start) == JsonNodeType.NUMBER;
		assert jsonProvider.getNodeType(end) == JsonNodeType.NULL || jsonProvider.getNodeType(end) == JsonNodeType.NUMBER;
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			Range r = Range.resolve(jsonProvider, start, end, jsonProvider.size(pobj));
			JsonNode subarray = jsonProvider.createArray();
			for (long index = r.start; index < r.end; ++index)
				jsonProvider.add(subarray, jsonProvider.requireGet(pobj, (int) index));
			output.emit(subarray, ArrayRangeIndexPath.chainIfNotNull(ppath, start, end, version));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.STRING) {
			Range r = Range.resolve(jsonProvider, start, end, UnicodeUtils.lengthUtf32(jsonProvider.asText(pobj)));
			JsonNode substring = jsonProvider.createString(UnicodeUtils.substringUtf32(jsonProvider.asText(pobj), (int) r.start, (int) r.end));
			output.emit(substring, ArrayRangeIndexPath.chainIfNotNull(ppath, start, end, version));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ArrayRangeIndexPath.chainIfNotNull(ppath, start, end, version));
		} else {
			if (!permissive) {
				JsonNode subpath = jsonProvider.createObject();
				jsonProvider.set(subpath, "start", start);
				jsonProvider.set(subpath, "end", end);
				throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, pobj, subpath));
			}
		}
	}
}
