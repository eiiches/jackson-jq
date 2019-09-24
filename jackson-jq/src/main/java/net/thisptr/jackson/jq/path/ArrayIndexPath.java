package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public class ArrayIndexPath implements Path {
	public final JsonNode index;
	private final Path parent;

	public static ArrayIndexPath chainIfNotNull(final Path parent, final int index) {
		return chainIfNotNull(parent, IntNode.valueOf(index));
	}

	public static ArrayIndexPath chainIfNotNull(final Path parent, final JsonNode index) {
		if (parent == null)
			return null;
		return new ArrayIndexPath(parent, index);
	}

	public ArrayIndexPath(final Path parent, final JsonNode index) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (index == null)
			throw new NullPointerException("index must not be null");
		if (!index.isNumber())
			throw new IllegalArgumentException("index must be a number");
		this.index = index;
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		parent.toJsonNode(out);
		out.add(index);
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			resolve(parent, ppath, output, index, permissive);
		}, permissive);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			return mutate(oldval, index, mutation, makeParent, !makeParent);
		}, makeParent);
	}

	private static JsonNode mutate(JsonNode in, final JsonNode index, final Mutation mutation, final boolean makeParent, final boolean deleteMode) throws JsonQueryException {
		assert index.isNumber();
		if (in == null || in.isNull()) {
			if (!makeParent)
				return in;
			in = MAPPER.createArrayNode();
		}
		if (in.isArray()) {
			final int indexAsInt = index.asInt();
			final int _index = indexAsInt < 0 ? indexAsInt + in.size() : indexAsInt;
			if (deleteMode && (_index < 0 || in.size() <= _index))
				return in;
			if (_index < 0)
				throw new JsonQueryException("Out of bounds negative array index");

			final JsonNode newval = mutation.apply(_index < in.size() ? in.get(_index) : null);
			if (newval == null)
				return in;

			final ArrayNode out = MAPPER.createArrayNode();
			for (int i = 0; i < in.size(); ++i)
				out.add(in.get(i));
			for (int i = in.size(); i <= _index; ++i)
				out.add(NullNode.getInstance());
			out.set(_index, newval);
			return out;
		} else {
			throw new JsonQueryTypeException("Cannot index %s with number", in.getNodeType());
		}
	}

	public static void resolve(final JsonNode pobj, final Path ppath, final PathOutput output, final JsonNode index, final boolean permissive) throws JsonQueryException {
		assert index.isNumber();
		if (pobj.isArray()) {
			final int indexAsInt = index.asInt();
			if (index.asDouble() != indexAsInt) { // if index is not an integer, emit null
				output.emit(NullNode.getInstance(), ArrayIndexPath.chainIfNotNull(ppath, index));
				return;
			}
			final int indexResolved = indexAsInt < 0 ? indexAsInt + pobj.size() : indexAsInt;
			if (indexResolved < 0 || pobj.size() <= indexResolved) { // out of range index
				output.emit(NullNode.getInstance(), ArrayIndexPath.chainIfNotNull(ppath, index));
				return;
			}
			output.emit(pobj.get(indexResolved), ArrayIndexPath.chainIfNotNull(ppath, index));
		} else if (pobj.isNull()) {
			output.emit(NullNode.getInstance(), ArrayIndexPath.chainIfNotNull(ppath, index));
		} else {
			if (!permissive)
				throw new JsonQueryTypeException("Cannot index %s with number", pobj.getNodeType());
		}
	}
}
