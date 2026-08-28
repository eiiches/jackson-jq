package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexOfPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
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
	protected final @Nullable Version version;

	public FieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> target, boolean permissive) {
		this(jsonProvider, target, permissive, null);
	}

	public FieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> target, boolean permissive, @Nullable Version version) {
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

	protected static <JsonNode> void emitAllPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking) throws JsonQueryException {
		emitAllPath(jsonProvider, permissive, pobj, ppath, output, tracking, null);
	}

	protected static <JsonNode> void emitAllPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, @Nullable Version version) throws JsonQueryException {
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to iterate through %s", JsonNodeUtils.toString(jsonProvider, pobj)));
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.size(pobj); ++i)
				output.emit(jsonProvider.requireGet(pobj, i), ArrayIndexPath.chainIfNotNull(jsonProvider, ppath, i, version));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(pobj);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), ObjectFieldPath.chainIfNotNull(ppath, entry.getKey(), version));
			}
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot iterate over %s", pobj);
		}
	}

	protected static <JsonNode> void emitObjectFieldPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking) throws JsonQueryException {
		emitObjectFieldPath(jsonProvider, permissive, key, pobj, ppath, output, tracking, null);
	}

	protected static <JsonNode> void emitObjectFieldPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, @Nullable Version version) throws JsonQueryException {
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, jsonProvider.createString(key)), JsonNodeUtils.toString(jsonProvider, pobj)));
		ObjectFieldPath.resolve(jsonProvider, pobj, ppath, output, key, permissive, version);
	}

	protected static <JsonNode> void emitArrayIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode index, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking) throws JsonQueryException {
		emitArrayIndexPath(jsonProvider, permissive, index, pobj, ppath, output, tracking, null);
	}

	protected static <JsonNode> void emitArrayIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode index, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, index), JsonNodeUtils.toString(jsonProvider, pobj)));
		ArrayIndexPath.resolve(jsonProvider, pobj, ppath, output, index, permissive, version);
	}

	protected static <JsonNode> void emitArrayIndexOfPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode subseqToLookFor, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking) throws JsonQueryException {
		emitArrayIndexOfPath(jsonProvider, permissive, subseqToLookFor, pobj, ppath, output, tracking, null);
	}

	protected static <JsonNode> void emitArrayIndexOfPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode subseqToLookFor, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseqToLookFor) == JsonNodeType.ARRAY;
		if (tracking && UnrepresentablePath.isLost(ppath))
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(jsonProvider, subseqToLookFor), JsonNodeUtils.toString(jsonProvider, pobj)));
		ArrayIndexOfPath.resolve(jsonProvider, pobj, ppath, output, subseqToLookFor, permissive, version);
	}

	protected static <JsonNode> void emitArrayRangeIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode start, JsonNode end, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking) throws JsonQueryException {
		emitArrayRangeIndexPath(jsonProvider, permissive, start, end, pobj, ppath, output, tracking, null);
	}

	protected static <JsonNode> void emitArrayRangeIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode start, JsonNode end, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, boolean tracking, @Nullable Version version) throws JsonQueryException {
		JsonNodeType startType = jsonProvider.getNodeType(start);
		JsonNodeType endType = jsonProvider.getNodeType(end);
		assert startType == JsonNodeType.NULL || startType == JsonNodeType.NUMBER;
		assert endType == JsonNodeType.NULL || endType == JsonNodeType.NUMBER;
		if (tracking && UnrepresentablePath.isLost(ppath)) {
			@Var JsonNode subpath = jsonProvider.createObject();
			subpath = jsonProvider.set(subpath, "start", start);
			subpath = jsonProvider.set(subpath, "end", end);
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", ExceptionMessages.truncate(JsonNodeUtils.toString(jsonProvider, subpath), version), JsonNodeUtils.toString(jsonProvider, pobj)));
		}
		ArrayRangeIndexPath.resolve(jsonProvider, pobj, ppath, output, start, end, permissive, version);
	}
}
