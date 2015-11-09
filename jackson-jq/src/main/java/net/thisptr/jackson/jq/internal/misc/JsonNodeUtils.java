package net.thisptr.jackson.jq.internal.misc;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedAllFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedEmptyFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedIndexFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedRangeFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class JsonNodeUtils {
	private JsonNodeUtils() {}

	public static boolean asBoolean(final JsonNode n) {
		if (n == null || n.isNull() || n.isMissingNode())
			return false;
		if (n.isBoolean())
			return n.asBoolean();
		return true;
	}

	public static boolean isIntegralNumber(final JsonNode n) {
		if (!n.isNumber())
			return false;
		if (n.asDouble() == n.asLong())
			return true;
		return false;
	}

	public static JsonNode asNumericNode(final long value) {
		if (((int) value) == value)
			return new IntNode((int) value);
		return new LongNode((long) value);
	}

	public static JsonNode asNumericNode(final double value) {
		if (((int) value) == value)
			return new IntNode((int) value);
		if (((long) value) == value)
			return new LongNode((long) value);
		return new DoubleNode(value);
	}

	public static ArrayNode asArrayNode(final ObjectMapper mapper, final List<JsonNode> values) {
		final ArrayNode result = mapper.createArrayNode();
		result.addAll(values);
		return result;
	}

	public static List<JsonNode> asArrayList(final ArrayNode in) {
		return Lists.newArrayList(in);
	}

	public static String typeOf(final JsonNode in) {
		if (in == null)
			return "null";
		switch (in.getNodeType()) {
			case ARRAY:
				return "array";
			case BINARY:
				return "string";
			case BOOLEAN:
				return "boolean";
			case MISSING:
				return "null";
			case NULL:
				return "null";
			case NUMBER:
				return "number";
			case OBJECT:
				return "object";
			case STRING:
				return "string";
			default:
				throw new IllegalArgumentException("Unknown JsonNodeType: " + in.getNodeType());
		}
	}

	public interface Mutation {
		JsonNode apply(JsonNode value) throws JsonQueryException;
	}

	public static void copy(final ArrayNode out, final JsonNode in) throws JsonQueryException {
		if (!in.isArray())
			throw new IllegalJsonArgumentException("input must be ARRAY");

		for (final JsonNode i : in)
			out.add(i);
	}

	public static void copy(final ObjectNode out, final JsonNode in) throws JsonQueryException {
		if (!in.isObject())
			throw new IllegalJsonArgumentException("input must be OBJECT");

		final Iterator<Entry<String, JsonNode>> iter = in.fields();
		while (iter.hasNext()) {
			final Entry<String, JsonNode> entry = iter.next();
			out.set(entry.getKey(), entry.getValue());
		}
	}

	public static JsonNode mutate(final ObjectMapper mapper, final JsonNode in, final List<ResolvedFieldAccess> path, final Mutation mutation, final boolean creative) throws JsonQueryException {
		if (path.isEmpty()) {
			return mutation.apply(in);
		} else {
			final ResolvedFieldAccess accessHead = path.get(0);
			final List<ResolvedFieldAccess> accessTail = path.subList(1, path.size());

			if (accessHead instanceof ResolvedEmptyFieldAccess) {
				return in;
			} else if (accessHead instanceof ResolvedIndexFieldAccess) {
				final ResolvedIndexFieldAccess access = (ResolvedIndexFieldAccess) accessHead;
				if (in.isNull()) {
					if (!creative)
						return NullNode.getInstance();
					final ArrayNode result = mapper.createArrayNode();
					for (final long index : access.indices()) {
						if (index < 0)
							throw new JsonQueryException("Out of bounds negative array index");
						while (index >= result.size())
							result.add(NullNode.getInstance());
						result.set((int) index, mutate(mapper, NullNode.getInstance(), accessTail, mutation, creative));
					}
					return result;
				} else if (in.isArray()) {
					final ArrayNode result = mapper.createArrayNode();
					final ArrayNode tmp = mapper.createArrayNode();
					copy(tmp, in);
					for (long index : access.indices()) {
						index = index >= 0 ? index : index + in.size();
						JsonNode value = tmp.get((int) index);
						if (value == null && creative)
							value = NullNode.getInstance();
						final JsonNode newvalue = mutate(mapper, value, accessTail, mutation, creative);
						if (newvalue != null) {
							while (index >= tmp.size())
								tmp.add(NullNode.getInstance());
							tmp.set((int) index, newvalue);
						} else {
							tmp.set((int) index, MissingNode.getInstance());
						}
					}
					for (final JsonNode t : tmp)
						if (!t.isMissingNode())
							result.add(t);
					return result;
				} else {
					if (!accessHead.permissive)
						throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
					return in;
				}
			} else if (accessHead instanceof ResolvedStringFieldAccess) {
				final ResolvedStringFieldAccess access = (ResolvedStringFieldAccess) accessHead;
				if (in.isNull()) {
					if (!creative)
						return NullNode.getInstance();
					final ObjectNode result = mapper.createObjectNode();
					for (final String key : access.keys())
						result.set(key, mutate(mapper, NullNode.getInstance(), accessTail, mutation, creative));
					return result;
				} else if (in.isObject()) {
					final ObjectNode result = mapper.createObjectNode();
					copy(result, in);
					for (final String key : access.keys()) {
						JsonNode value = result.get(key);
						if (value == null && creative)
							value = NullNode.getInstance();
						final JsonNode newvalue = mutate(mapper, value, accessTail, mutation, creative);
						if (newvalue == null) {
							result.remove(key);
						} else if (newvalue != value) {
							result.set(key, newvalue);
						}
					}
					return result;
				} else {
					if (!accessHead.permissive)
						throw JsonQueryException.format("Cannot index %s with string \"%s\"", in.getNodeType(), access.keys().get(0));
					return in;
				}
			} else if (accessHead instanceof ResolvedRangeFieldAccess) {
				final ResolvedRangeFieldAccess access = (ResolvedRangeFieldAccess) accessHead;
				if (in.isNull()) {
					final JsonNode newvalue = mutate(mapper, NullNode.getInstance(), accessTail, mutation, creative);
					if (!newvalue.isArray())
						throw new JsonQueryException("A slice of an array can only be assigned another array");
					return newvalue;
				} else if (in.isArray()) {
					ArrayNode result = mapper.createArrayNode();
					copy(result, in);
					final ArrayNode tmp = mapper.createArrayNode();
					for (Range range : access.ranges()) {
						range = range.over(result.size());

						for (int i = 0; i < range.begin; ++i)
							tmp.add(result.get(i));
						final ArrayNode slice = mapper.createArrayNode();
						for (int i = (int) range.begin; i < range.end; ++i)
							slice.add(result.get(i));
						final JsonNode newvalue = mutate(mapper, slice, accessTail, mutation, creative);
						if (newvalue != null && newvalue.isArray()) {
							for (final JsonNode t : newvalue)
								tmp.add(t);
						} else {
							if (newvalue != null)
								throw new JsonQueryException("A slice of an array can only be assigned another array");
						}
						for (int i = (int) range.end; i < result.size(); ++i)
							tmp.add(result.get(i));
						result = tmp;
					}
					return result;
				} else if (in.isTextual()) {
					throw new JsonQueryException("Cannot update field at object index of string");
				} else {
					if (!accessHead.permissive)
						throw JsonQueryException.format("Cannot index %s with object", in.getNodeType());
					return in;
				}
			} else if (accessHead instanceof ResolvedAllFieldAccess) {
				if (in.isNull()) {
					throw new JsonQueryException("Cannot iterate over null");
				} else if (in.isObject()) {
					final ObjectNode result = mapper.createObjectNode();
					final Iterator<Entry<String, JsonNode>> iter = in.fields();
					while (iter.hasNext()) {
						final Entry<String, JsonNode> entry = iter.next();
						final String key = entry.getKey();
						final JsonNode value = entry.getValue();
						final JsonNode newvalue = mutate(mapper, value, accessTail, mutation, creative);
						if (newvalue != null)
							result.set(key, newvalue);
					}
					return result;
				} else if (in.isArray()) {
					final ArrayNode result = mapper.createArrayNode();
					for (int key = 0; key < in.size(); ++key) {
						final JsonNode value = in.get(key);
						final JsonNode newvalue = mutate(mapper, value, accessTail, mutation, creative);
						if (newvalue != null)
							result.add(newvalue);
					}
					return result;
				} else {
					throw JsonQueryException.format("Cannot iterate over %s", in.getNodeType());
				}
			}
			throw new IllegalStateException();
		}
	}
}
