package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexOfPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class FieldAccess<JsonNode> implements Expression<JsonNode> {
	protected final JsonProvider<JsonNode> jsonProvider;
	protected final Expression<JsonNode> target;
	protected final boolean permissive;

	public FieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> target, boolean permissive) {
		this.jsonProvider = jsonProvider;
		this.target = target;
		this.permissive = permissive;
	}

	public Expression<JsonNode> target() {
		return target;
	}

	public boolean permissive() {
		return permissive;
	}

	protected static <JsonNode> void emitAllPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, boolean tracking) throws JsonQueryException {
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException("Invalid path expression near attempt to iterate through %s", JsonNodeUtils.toString(jsonProvider, pobj));
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.size(pobj); ++i)
				output.emit(jsonProvider.requireGet(pobj, i), ArrayIndexPath.chainIfNotNull(jsonProvider, ppath, i));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(pobj);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), ObjectFieldPath.chainIfNotNull(ppath, entry.getKey()));
			}
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", pobj);
		}
	}

	protected static <JsonNode> void emitObjectFieldPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, boolean tracking) throws JsonQueryException {
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, jsonProvider.createString(key)), JsonNodeUtils.toString(jsonProvider, pobj));
		ObjectFieldPath.resolve(jsonProvider, pobj, ppath, output, key, permissive);
	}

	protected static <JsonNode> void emitArrayIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode index, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, boolean tracking) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, index), JsonNodeUtils.toString(jsonProvider, pobj));
		ArrayIndexPath.resolve(jsonProvider, pobj, ppath, output, index, permissive);
	}

	protected static <JsonNode> void emitArrayIndexOfPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode subseqToLookFor, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, boolean tracking) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseqToLookFor) == JsonNodeType.ARRAY;
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, subseqToLookFor), JsonNodeUtils.toString(jsonProvider, pobj));
		ArrayIndexOfPath.resolve(jsonProvider, pobj, ppath, output, subseqToLookFor, permissive);
	}

	protected static <JsonNode> void emitArrayRangeIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode start, JsonNode end, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, boolean tracking) throws JsonQueryException {
		JsonNodeType startType = jsonProvider.getNodeType(start);
		JsonNodeType endType = jsonProvider.getNodeType(end);
		assert startType == JsonNodeType.NULL || startType == JsonNodeType.NUMBER;
		assert endType == JsonNodeType.NULL || endType == JsonNodeType.NUMBER;
		if (tracking && UnrepresentablePath.isLost(ppath)) {
			JsonNode subpath = jsonProvider.createObject();
			jsonProvider.set(subpath, "start", start);
			jsonProvider.set(subpath, "end", end);
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", Strings.truncate(JsonNodeUtils.toString(jsonProvider, subpath), 14), JsonNodeUtils.toString(jsonProvider, pobj));
		}
		ArrayRangeIndexPath.resolve(jsonProvider, pobj, ppath, output, start, end, permissive);
	}
}
