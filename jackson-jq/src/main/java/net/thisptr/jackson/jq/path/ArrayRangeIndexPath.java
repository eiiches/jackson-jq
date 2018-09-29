package net.thisptr.jackson.jq.path;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Range;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

/**
 * Despite the name, {@link ArrayRangeIndexPath} can be used to index strings.
 */
public class ArrayRangeIndexPath implements Path {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public final Long start;
	public final Long end;
	private final Path parent;

	public static ArrayRangeIndexPath chainIfNotNull(final Path parent, final Long start, final Long end) {
		if (parent == null)
			return null;
		return new ArrayRangeIndexPath(parent, start, end);
	}

	public ArrayRangeIndexPath(final Path parent, final Long start, final Long end) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
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
		range.set("start", start == null ? NullNode.getInstance() : LongNode.valueOf(start));
		range.set("end", end == null ? NullNode.getInstance() : LongNode.valueOf(end));
		parent.toJsonNode(out);
		out.add(range);
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			final Optional<JsonNode> out = resolve(parent, start, end, permissive);
			if (out.isPresent())
				output.emit(out.get(), ArrayRangeIndexPath.chainIfNotNull(ppath, start, end));
		}, permissive);
	}

	private static JsonNode mutate(JsonNode in, final Long start, final Long end, final Mutation mutation) throws JsonQueryException {
		if (in == null)
			in = NullNode.getInstance();
		if (in.isArray()) {
			final Range r = new Range(start, end).over(in.size());
			final ArrayNode out = MAPPER.createArrayNode();
			for (int index = 0; index < r.start; ++index)
				out.add(in.get(index));

			final ArrayNode oldval = MAPPER.createArrayNode();
			for (long index = r.start; index < r.end; ++index)
				oldval.add(in.get((int) index));
			final JsonNode newval = mutation.apply(oldval);
			if (!newval.isArray())
				throw new JsonQueryException("A slice of an array can only be assigned another array");
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
				throw new JsonQueryException("A slice of an array can only be assigned another array");
			return newval;
		} else {
			throw JsonQueryException.format("Cannot index %s with object", in.getNodeType());
		}
	}

	public static Optional<JsonNode> resolve(JsonNode pobj, final Long start, final Long end, boolean permissive) throws JsonQueryException {
		if (pobj.isArray()) {
			final Range r = new Range(start, end).over(pobj.size());
			final ArrayNode subarray = MAPPER.createArrayNode();
			for (long index = r.start; index < r.end; ++index)
				subarray.add(pobj.get((int) index));
			return Optional.of(subarray);
		} else if (pobj.isTextual()) {
			final Range r = new Range(start, end).over(UnicodeUtils.lengthUtf32(pobj.textValue()));
			return Optional.of(new TextNode(UnicodeUtils.substringUtf32(pobj.textValue(), (int) r.start.longValue(), (int) r.end.longValue())));
		} else if (pobj.isNull()) {
			return Optional.of(NullNode.getInstance());
		} else {
			if (!permissive)
				throw JsonQueryException.format("Cannot index %s with object", pobj.getNodeType());
			return Optional.empty();
		}
	}
}
