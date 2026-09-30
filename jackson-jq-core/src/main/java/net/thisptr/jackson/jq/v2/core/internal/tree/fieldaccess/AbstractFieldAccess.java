package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractFieldAccess<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	protected final JsonProvider<JsonNode> jsonProvider;
	protected final AnalyzedExpression<JsonNode> target;
	protected final boolean permissive;
	protected final Version version;
	protected final int targetOutputIndex;

	public AbstractFieldAccess(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> target, boolean permissive, Version version, int targetOutputIndex) {
		this.targetOutputIndex = targetOutputIndex;
		this.jsonProvider = jsonProvider;
		this.target = target;
		this.permissive = permissive;
		this.version = version;
	}

	public AnalyzedExpression<JsonNode> target() {
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

	private static <JsonNode> String describeKey(JsonProvider<JsonNode> jsonProvider, JsonNode key, Version version) {
		return ExceptionMessages.truncate(JsonNodeUtils.toString(jsonProvider, key, version), version, ExceptionMessages.SHORT_BUFFER_SIZE);
	}

	private static <JsonNode> String describeTarget(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, Version version) {
		return ExceptionMessages.truncate(JsonNodeUtils.toString(jsonProvider, pobj, version), version, ExceptionMessages.LONG_BUFFER_SIZE);
	}

	protected static <JsonNode> void emitAllPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, Version version) throws JsonQueryException {
		@Nullable Path<JsonNode> path = PathUtils.recover(jsonProvider, version, ppath, pobj);
		if (path == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to iterate through %s", describeTarget(jsonProvider, pobj, version)));
		if (jsonProvider.isNull(pobj)) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (jsonProvider.isArray(pobj)) {
			for (int i = 0; i < jsonProvider.getArrayLength(pobj); ++i)
				output.emit(jsonProvider.getArrayElement(pobj, i), path.appendIndex(i));
		} else if (jsonProvider.isObject(pobj)) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(pobj);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), path.appendKey(entry.getKey()));
			}
		} else {
			if (!permissive)
				throw new JsonQueryTypeException("Cannot iterate over %s", ExceptionMessages.describe(jsonProvider, version, pobj));
		}
	}

	protected static <JsonNode> void emitObjectFieldPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, String key, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, Version version) throws JsonQueryException {
		@Nullable Path<JsonNode> path = PathUtils.recover(jsonProvider, version, ppath, pobj);
		if (path == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", describeKey(jsonProvider, jsonProvider.createString(key), version), describeTarget(jsonProvider, pobj, version)));
		PathOperations.resolveObjectField(jsonProvider, pobj, path, output, key, permissive, version);
	}

	protected static <JsonNode> void emitArrayIndexPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode index, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, Version version) throws JsonQueryException {
		assert jsonProvider.isNumber(index);
		@Nullable Path<JsonNode> path = PathUtils.recover(jsonProvider, version, ppath, pobj);
		if (path == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", describeKey(jsonProvider, index, version), describeTarget(jsonProvider, pobj, version)));
		PathOperations.resolveArrayIndex(jsonProvider, pobj, path, output, index, permissive, version);
	}

	protected static <JsonNode> void emitIndexOfPath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode subseqToLookFor, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, Version version) throws JsonQueryException {
		assert jsonProvider.isArray(subseqToLookFor);
		@Nullable Path<JsonNode> path = PathUtils.recover(jsonProvider, version, ppath, pobj);
		if (path == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", describeKey(jsonProvider, subseqToLookFor, version), describeTarget(jsonProvider, pobj, version)));
		PathOperations.resolveArrayIndexOf(jsonProvider, pobj, path, output, subseqToLookFor, permissive, version);
	}

	protected static <JsonNode> void emitIndexRangePath(JsonProvider<JsonNode> jsonProvider, boolean permissive, JsonNode start, JsonNode end, JsonNode pobj, Path<JsonNode> ppath, Output<JsonNode> output, Version version) throws JsonQueryException {
		@Nullable Path<JsonNode> path = PathUtils.recover(jsonProvider, version, ppath, pobj);
		if (path == null) {
			Map<String, JsonNode> subpath = new LinkedHashMap<>();
			subpath.put("start", start);
			subpath.put("end", end);
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", describeKey(jsonProvider, jsonProvider.createObject(subpath), version), describeTarget(jsonProvider, pobj, version)));
		}
		PathOperations.resolveArrayRangeIndex(jsonProvider, pobj, path, output, start, end, permissive, version);
	}
}
