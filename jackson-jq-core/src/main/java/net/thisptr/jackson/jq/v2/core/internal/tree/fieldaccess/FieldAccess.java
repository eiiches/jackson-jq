package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class FieldAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	protected final JsonProvider<JsonNode> jsonProvider;
	protected final Expression<StackFrame, JsonNode> target;
	protected final boolean permissive;
	protected final Version version;

	public FieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> target, boolean permissive, Version version) {
		this.jsonProvider = jsonProvider;
		this.target = target;
		this.permissive = permissive;
		this.version = version;
	}

	public Expression<StackFrame, JsonNode> target() {
		return target;
	}

	public boolean permissive() {
		return permissive;
	}

	@Override
	public boolean dependsOnInput() {
		return target.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return target.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(target);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(target);
	}

	protected static <JsonNode> void emitAllPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, Version version) throws JsonQueryException {
		if (tracking && PathUtils.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to iterate through %s", JsonNodeUtils.toString(jsonProvider, pobj)));
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.getArrayLength(pobj); ++i)
				output.emit(jsonProvider.getArrayElement(pobj, i), ppath.appendIndex(i));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectEntries(pobj);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), ppath.appendKey(entry.getKey()));
			}
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot iterate over %s", pobj);
		}
	}

	protected static <JsonNode> void emitObjectFieldPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, Version version) throws JsonQueryException {
		if (tracking && PathUtils.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, jsonProvider.createString(key)), JsonNodeUtils.toString(jsonProvider, pobj)));
		PathOperations.resolveObjectField(jsonProvider, pobj, ppath, output, key, permissive, version);
	}

	protected static <JsonNode> void emitArrayIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode index, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (tracking && PathUtils.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, index), JsonNodeUtils.toString(jsonProvider, pobj)));
		PathOperations.resolveArrayIndex(jsonProvider, pobj, ppath, output, index, permissive, version);
	}

	protected static <JsonNode> void emitIndexOfPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode subseqToLookFor, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseqToLookFor) == JsonNodeType.ARRAY;
		if (tracking && PathUtils.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, subseqToLookFor), JsonNodeUtils.toString(jsonProvider, pobj)));
		PathOperations.resolveArrayIndexOf(jsonProvider, pobj, ppath, output, subseqToLookFor, permissive, version);
	}

	protected static <JsonNode> void emitIndexRangePath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode start, JsonNode end, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, Version version) throws JsonQueryException {
		JsonNodeType startType = jsonProvider.getNodeType(start);
		JsonNodeType endType = jsonProvider.getNodeType(end);
		assert startType == JsonNodeType.NULL || startType == JsonNodeType.NUMBER;
		assert endType == JsonNodeType.NULL || endType == JsonNodeType.NUMBER;
		if (tracking && PathUtils.isLost(ppath)) {
			Map<String, JsonNode> subpath = new LinkedHashMap<>();
			subpath.put("start", start);
			subpath.put("end", end);
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", ExceptionMessages.truncate(JsonNodeUtils.toString(jsonProvider, jsonProvider.createObject(subpath)), version), JsonNodeUtils.toString(jsonProvider, pobj)));
		}
		PathOperations.resolveArrayRangeIndex(jsonProvider, pobj, ppath, output, start, end, permissive, version);
	}
}
