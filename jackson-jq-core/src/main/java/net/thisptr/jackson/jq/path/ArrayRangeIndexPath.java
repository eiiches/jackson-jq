package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Range;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

/**
 * Despite the name, {@link ArrayRangeIndexPath} can be used to index strings.
 */
public class ArrayRangeIndexPath implements Path {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public final JsonNode start;
	public final JsonNode end;
	private final Path parent;

	public static ArrayRangeIndexPath chainIfNotNull(final Path parent, final JsonNode start, final JsonNode end) {
		if (parent == null)
			return null;
		return new ArrayRangeIndexPath(parent, start, end);
	}

	public ArrayRangeIndexPath(final Path parent, final JsonNode start, final JsonNode end) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		if (start == null)
			throw new NullPointerException("start must not be null");
		if (end == null)
			throw new NullPointerException("end must not be null");
		if (!start.isNumber() && !start.isNull())
			throw new IllegalArgumentException("start must be java null or json number");
		if (!end.isNumber() && !end.isNull())
			throw new IllegalArgumentException("end must be java null or json number");
		this.parent = parent;
		this.start = start;
		this.end = end;
	}

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			return mutate(oldval, start, end, mutation);
		}, makeParent);
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		final ObjectNode range = MAPPER.createObjectNode();
		range.set("start", start);
		range.set("end", end);
		parent.toJsonNode(out);
		out.add(range);
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			resolve(parent, ppath, output, start, end, permissive);
		}, permissive);
	}

	private static JsonNode mutate(JsonNode in, final JsonNode start, final JsonNode end, final Mutation mutation) throws JsonQueryException {
		assert start.isNull() || start.isNumber();
		assert end.isNull() || end.isNumber();
		if (in == null)
			in = NullNode.getInstance();
		if (in.isArray()) {
			final Range r = Range.resolve(start, end, in.size());
			final ArrayNode out = MAPPER.createArrayNode();
			for (int index = 0; index < r.start; ++index)
				out.add(in.get(index));

			final ArrayNode oldval = MAPPER.createArrayNode();
			for (long index = r.start; index < r.end; ++index)
				oldval.add(in.get((int) index));
			final JsonNode newval = mutation.apply(oldval);
			if (!newval.isArray())
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");
			for (final JsonNode element : newval)
				out.add(element);
			for (long index = r.end; index < in.size(); ++index)
				out.add(in.get((int) index));

			return out;
		} else if (in.isTextual()) {
			throw new JsonQueryException("Cannot update field at object index of string");
		} else if (in.isNull()) {
			final JsonNode newval = mutation.apply(NullNode.getInstance());
			if (!newval.isArray())
				throw new JsonQueryTypeException("A slice of an array can only be assigned another array");
			return newval;
		} else {
			throw new JsonQueryTypeException("Cannot index %s with object", in.getNodeType());
		}
	}

	public static void resolve(final JsonNode pobj, final Path ppath, final PathOutput output, final JsonNode start, final JsonNode end, final boolean permissive) throws JsonQueryException {
		assert start.isNull() || start.isNumber();
		assert end.isNull() || end.isNumber();
		if (pobj.isArray()) {
			final Range r = Range.resolve(start, end, pobj.size());
			final ArrayNode subarray = MAPPER.createArrayNode();
			for (long index = r.start; index < r.end; ++index)
				subarray.add(pobj.get((int) index));
			output.emit(subarray, ArrayRangeIndexPath.chainIfNotNull(ppath, start, end));
		} else if (pobj.isTextual()) {
			final Range r = Range.resolve(start, end, UnicodeUtils.lengthUtf32(pobj.textValue()));
			final TextNode substring = new TextNode(UnicodeUtils.substringUtf32(pobj.textValue(), (int) r.start, (int) r.end));
			output.emit(substring, ArrayRangeIndexPath.chainIfNotNull(ppath, start, end));
		} else if (pobj.isNull()) {
			output.emit(NullNode.getInstance(), ArrayRangeIndexPath.chainIfNotNull(ppath, start, end));
		} else {
			if (!permissive)
				throw new JsonQueryTypeException("Cannot index %s with object", pobj.getNodeType());
		}
	}
}
