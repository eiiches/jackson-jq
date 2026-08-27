package net.thisptr.jackson.jq.v2.core.path;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ArrayIndexPath<JsonNode> implements Path<JsonNode> {
	public final JsonNode index;
	private final Path<JsonNode> parent;
	private final @Nullable Version version;

	public static <JsonNode> @Nullable ArrayIndexPath<JsonNode> chainIfNotNull(JsonProvider<JsonNode> jsonProvider, @Nullable Path<JsonNode> parent, int index) {
		return chainIfNotNull(parent, jsonProvider.createNumber(index), null);
	}

	public static <JsonNode> @Nullable ArrayIndexPath<JsonNode> chainIfNotNull(JsonProvider<JsonNode> jsonProvider, @Nullable Path<JsonNode> parent, int index, @Nullable Version version) {
		return chainIfNotNull(parent, jsonProvider.createNumber(index), version);
	}

	public static <JsonNode> @Nullable ArrayIndexPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode index) {
		return chainIfNotNull(parent, index, null);
	}

	public static <JsonNode> @Nullable ArrayIndexPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode index, @Nullable Version version) {
		if (parent == null)
			return null;
		return new ArrayIndexPath<>(parent, index, version);
	}

	public ArrayIndexPath(Path<JsonNode> parent, JsonNode index) {
		this(parent, index, null);
	}

	public ArrayIndexPath(Path<JsonNode> parent, JsonNode index, @Nullable Version version) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (index == null)
			throw new NullPointerException("index must not be null");
		// Note: cannot validate isNumber() without JsonProvider here
		this.index = index;
		this.version = version;
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, index);
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, index, permissive, version);
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, index, mutation, makeParent, !makeParent, version);
		}, makeParent);
	}

	private static <JsonNode> @Nullable JsonNode mutate(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, JsonNode index, Mutation<JsonNode> mutation, boolean makeParent, boolean deleteMode, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (in == null || jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			if (!makeParent)
				return in;
			in = jsonProvider.createArray();
		}
		if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			double indexAsDouble = jsonProvider.asDouble(index);
			if (Double.isNaN(indexAsDouble) || Double.isInfinite(indexAsDouble))
				throw new JsonQueryException("Cannot use " + (Double.isNaN(indexAsDouble) ? "nan" : "infinite") + " as array index");
			// Truncate fractional indices to int (jq behavior)
			int indexAsInt = (int) indexAsDouble;
			int _index = indexAsInt < 0 ? indexAsInt + jsonProvider.size(in) : indexAsInt;
			if (deleteMode && (_index < 0 || jsonProvider.size(in) <= _index))
				return in;
			if (_index < 0)
				throw new JsonQueryException("Out of bounds negative array index");

			JsonNode newval = mutation.apply(_index < jsonProvider.size(in) ? jsonProvider.requireGet(in, _index) : null);
			if (newval == null)
				return in;

			JsonNode out = jsonProvider.createArray();
			for (int i = 0; i < jsonProvider.size(in); ++i)
				jsonProvider.add(out, jsonProvider.requireGet(in, i));
			for (int i = jsonProvider.size(in); i <= _index; ++i)
				jsonProvider.add(out, jsonProvider.createNull());
			jsonProvider.set(out, _index, newval);
			return out;
		} else {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, index));
		}
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, JsonNode index, boolean permissive) throws JsonQueryException {
		resolve(jsonProvider, pobj, ppath, output, index, permissive, null);
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, JsonNode index, boolean permissive, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			double indexAsDouble = jsonProvider.asDouble(index);
			// if index is not a valid integer (NaN, Infinity, or fractional), emit null
			if (Double.isNaN(indexAsDouble) || Double.isInfinite(indexAsDouble)) {
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index, version));
				return;
			}
			int indexAsInt = (int) indexAsDouble;
			if (indexAsDouble != indexAsInt) {
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index, version));
				return;
			}
			int indexResolved = indexAsInt < 0 ? indexAsInt + jsonProvider.size(pobj) : indexAsInt;
			if (indexResolved < 0 || jsonProvider.size(pobj) <= indexResolved) { // out of range index
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index, version));
				return;
			}
			output.emit(jsonProvider.requireGet(pobj, indexResolved), ArrayIndexPath.chainIfNotNull(ppath, index, version));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index, version));
		} else {
			if (!permissive)
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, pobj, index));
		}
	}
}
