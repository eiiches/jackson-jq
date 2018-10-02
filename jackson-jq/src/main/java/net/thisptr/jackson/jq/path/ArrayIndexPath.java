package net.thisptr.jackson.jq.path;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

/**
 * Despite the name, {@link ArrayIndexPath} can be used to index strings.
 */
public class ArrayIndexPath implements Path {
	public final int index;
	private final Path parent;

	public static ArrayIndexPath chainIfNotNull(final Path parent, final int index) {
		if (parent == null)
			return null;
		return new ArrayIndexPath(parent, index);
	}

	public ArrayIndexPath(final Path parent, final int index) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		this.index = index;
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		parent.toJsonNode(out);
		out.add(IntNode.valueOf(index));
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			final Optional<JsonNode> out = resolve(parent, index, permissive);
			if (out.isPresent())
				output.emit(out.get(), ArrayIndexPath.chainIfNotNull(ppath, index));
		}, permissive);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			return mutate(oldval, index, mutation, makeParent, !makeParent);
		}, makeParent);
	}

	private static JsonNode mutate(JsonNode in, final int index, final Mutation mutation, final boolean makeParent, final boolean deleteMode) throws JsonQueryException {
		if (in == null || in.isNull()) {
			if (!makeParent)
				return in;
			in = MAPPER.createArrayNode();
		}
		if (in.isArray()) {
			final int _index = index < 0 ? index + in.size() : index;
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

	public static Optional<JsonNode> resolve(final JsonNode pobj, final int index, final boolean permissive) throws JsonQueryException {
		if (pobj.isArray()) {
			final int _index = index < 0 ? index + pobj.size() : index;
			if (0 <= _index && _index < pobj.size()) {
				return Optional.of(pobj.get(_index));
			} else {
				return Optional.of(NullNode.getInstance());
			}
		} else if (pobj.isNull()) {
			return Optional.of(NullNode.getInstance());
		} else {
			if (!permissive)
				throw new JsonQueryTypeException("Cannot index %s with number", pobj.getNodeType());
			return Optional.empty();
		}
	}
}
