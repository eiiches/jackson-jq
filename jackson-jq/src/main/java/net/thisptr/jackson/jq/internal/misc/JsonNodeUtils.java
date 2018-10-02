package net.thisptr.jackson.jq.internal.misc;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.JsonQueryException;

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
		return n.asDouble() == n.asLong();
	}

	public static JsonNode asNumericNode(final long value) {
		if (((int) value) == value)
			return new IntNode((int) value);
		return new LongNode(value);
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

	public static JsonNode nullToNullNode(final JsonNode value) {
		if (value == null)
			return NullNode.getInstance();
		return value;
	}

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.registerModule(JsonQueryJacksonModule.getInstance());

	private static JsonNode filterInternal(final JsonNode in, final Predicate<JsonNode> pred) {
		if (in.isObject()) {
			final ObjectNode out = MAPPER.createObjectNode();
			final Iterator<Entry<String, JsonNode>> iter = in.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				if (!pred.test(entry.getValue()))
					continue;
				out.set(entry.getKey(), filterInternal(entry.getValue(), pred));
			}
			return out;
		} else if (in.isArray()) {
			final ArrayNode out = MAPPER.createArrayNode();
			final Iterator<JsonNode> iter = in.iterator();
			while (iter.hasNext()) {
				final JsonNode val = iter.next();
				if (!pred.test(val))
					continue;
				out.add(filterInternal(val, pred));
			}
			return out;
		} else {
			return in;
		}
	}

	public static JsonNode filter(final JsonNode in, final Predicate<JsonNode> pred) {
		if (!pred.test(in))
			return NullNode.getInstance();
		return filterInternal(in, pred);
	}

	public static String toString(final JsonNode node) {
		try {
			return MAPPER.writeValueAsString(node);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
