package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.ArrayIndexOfPath;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public abstract class FieldAccess<JsonNode> implements Expression<JsonNode> {
	protected final Expression<JsonNode> target;
	protected final boolean permissive;

	public FieldAccess(final Expression<JsonNode> target, final boolean permissive) {
		this.target = target;
		this.permissive = permissive;
	}

	protected static <JsonNode> void emitAllPath(final JsonProvider<JsonNode> jsonProvider, final boolean permissive, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to iterate through %s", JsonNodeUtils.toString(jsonProvider, pobj));
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.size(pobj); ++i)
				output.emit(jsonProvider.get(pobj, i), ArrayIndexPath.chainIfNotNull(jsonProvider, ppath, i));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(pobj);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), ObjectFieldPath.chainIfNotNull(ppath, entry.getKey()));
			}
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", pobj);
		}
	}

	protected static <JsonNode> void emitObjectFieldPath(final JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, jsonProvider.createString(key)), JsonNodeUtils.toString(jsonProvider, pobj));
		ObjectFieldPath.resolve(jsonProvider, pobj, ppath, output, key, permissive);
	}

	protected static <JsonNode> void emitArrayIndexPath(final JsonProvider<JsonNode> jsonProvider, boolean permissive, final JsonNode index, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, index), JsonNodeUtils.toString(jsonProvider, pobj));
		ArrayIndexPath.resolve(jsonProvider, pobj, ppath, output, index, permissive);
	}

	protected static <JsonNode> void emitArrayIndexOfPath(final JsonProvider<JsonNode> jsonProvider, boolean permissive, final JsonNode subseqToLookFor, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseqToLookFor) == JsonNodeType.ARRAY;
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, subseqToLookFor), JsonNodeUtils.toString(jsonProvider, pobj));
		ArrayIndexOfPath.resolve(jsonProvider, pobj, ppath, output, subseqToLookFor, permissive);
	}

	protected static <JsonNode> void emitArrayRangeIndexPath(final JsonProvider<JsonNode> jsonProvider, boolean permissive, final JsonNode start, final JsonNode end, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		JsonNodeType startType = jsonProvider.getNodeType(start);
		JsonNodeType endType = jsonProvider.getNodeType(end);
		assert startType == JsonNodeType.NULL || startType == JsonNodeType.NUMBER;
		assert endType == JsonNodeType.NULL || endType == JsonNodeType.NUMBER;
		if (requirePath && ppath == null) {
			final JsonNode subpath = jsonProvider.createObject();
			jsonProvider.set(subpath, "start", start);
			jsonProvider.set(subpath, "end", end);
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", Strings.truncate(JsonNodeUtils.toString(jsonProvider, subpath), 14), JsonNodeUtils.toString(jsonProvider, pobj));
		}
		ArrayRangeIndexPath.resolve(jsonProvider, pobj, ppath, output, start, end, permissive);
	}
}
