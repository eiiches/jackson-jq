package net.thisptr.jackson.jq.path;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public class ArrayIndexPath<JsonNode> implements Path<JsonNode> {
	public final JsonNode index;
	private final Path<JsonNode> parent;

	public static <JsonNode> ArrayIndexPath<JsonNode> chainIfNotNull(final JsonProvider<JsonNode> jsonProvider, final Path<JsonNode> parent, final int index) {
		return chainIfNotNull(parent, jsonProvider.createNumber(index));
	}

	public static <JsonNode> ArrayIndexPath<JsonNode> chainIfNotNull(final Path<JsonNode> parent, final JsonNode index) {
		if (parent == null)
			return null;
		return new ArrayIndexPath<>(parent, index);
	}

	public ArrayIndexPath(final Path<JsonNode> parent, final JsonNode index) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (index == null)
			throw new NullPointerException("index must not be null");
		// Note: cannot validate isNumber() without JsonProvider here
		this.index = index;
	}

	@Override
	public void toJsonNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, index);
	}

	@Override
	public void get(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, index, permissive);
		}, permissive);
	}

	@Override
	public JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, index, mutation, makeParent, !makeParent);
		}, makeParent);
	}

	private static <JsonNode> JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, JsonNode in, final JsonNode index, final Mutation<JsonNode> mutation, final boolean makeParent, final boolean deleteMode) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (in == null || jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			if (!makeParent)
				return in;
			in = jsonProvider.createArray();
		}
		if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			final double indexAsDouble = jsonProvider.asDouble(index);
			if (Double.isNaN(indexAsDouble) || Double.isInfinite(indexAsDouble))
				throw new JsonQueryException("Cannot use " + (Double.isNaN(indexAsDouble) ? "nan" : "infinite") + " as array index");
			// Truncate fractional indices to int (jq behavior)
			final int indexAsInt = (int) indexAsDouble;
			final int _index = indexAsInt < 0 ? indexAsInt + jsonProvider.size(in) : indexAsInt;
			if (deleteMode && (_index < 0 || jsonProvider.size(in) <= _index))
				return in;
			if (_index < 0)
				throw new JsonQueryException("Out of bounds negative array index");

			final JsonNode newval = mutation.apply(_index < jsonProvider.size(in) ? jsonProvider.get(in, _index) : null);
			if (newval == null)
				return in;

			final JsonNode out = jsonProvider.createArray();
			for (int i = 0; i < jsonProvider.size(in); ++i)
				jsonProvider.add(out, jsonProvider.get(in, i));
			for (int i = jsonProvider.size(in); i <= _index; ++i)
				jsonProvider.add(out, jsonProvider.createNull());
			jsonProvider.set(out, _index, newval);
			return out;
		} else {
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", jsonProvider.getNodeType(in));
		}
	}

	public static <JsonNode> void resolve(final JsonProvider<JsonNode> jsonProvider, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final JsonNode index, final boolean permissive) throws JsonQueryException {
		assert jsonProvider.getNodeType(index) == JsonNodeType.NUMBER;
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			final double indexAsDouble = jsonProvider.asDouble(index);
			// if index is not a valid integer (NaN, Infinity, or fractional), emit null
			if (Double.isNaN(indexAsDouble) || Double.isInfinite(indexAsDouble)) {
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index));
				return;
			}
			final int indexAsInt = (int) indexAsDouble;
			if (indexAsDouble != indexAsInt) {
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index));
				return;
			}
			final int indexResolved = indexAsInt < 0 ? indexAsInt + jsonProvider.size(pobj) : indexAsInt;
			if (indexResolved < 0 || jsonProvider.size(pobj) <= indexResolved) { // out of range index
				output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index));
				return;
			}
			output.emit(jsonProvider.get(pobj, indexResolved), ArrayIndexPath.chainIfNotNull(ppath, index));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ArrayIndexPath.chainIfNotNull(ppath, index));
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", jsonProvider.getNodeType(pobj));
		}
	}
}
