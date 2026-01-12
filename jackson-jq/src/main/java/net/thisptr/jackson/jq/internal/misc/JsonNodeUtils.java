package net.thisptr.jackson.jq.internal.misc;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class JsonNodeUtils {
	private JsonNodeUtils() {}

	public static <JsonNode> boolean asBoolean(final JsonProvider<JsonNode> jsonProvider, final JsonNode n) {
		if (n == null || jsonProvider.getNodeType(n) == JsonNodeType.NULL || jsonProvider.isMissingNode(n))
			return false;
		if (jsonProvider.getNodeType(n) == JsonNodeType.BOOLEAN)
			return jsonProvider.asBoolean(n);
		return true;
	}

	public static <JsonNode> JsonNode asNumericNode(final JsonProvider<JsonNode> jsonProvider, final long value) {
		if (((int) value) == value)
			return jsonProvider.createInt((int) value);
		return jsonProvider.createLong(value);
	}

	public static <JsonNode> JsonNode asNumericNode(final JsonProvider<JsonNode> jsonProvider, final double value) {
		if (((int) value) == value)
			return jsonProvider.createInt((int) value);
		if (((long) value) == value)
			return jsonProvider.createLong((long) value);
		return jsonProvider.createDouble(value);
	}

	public static <JsonNode> JsonNode asArrayNode(final JsonProvider<JsonNode> jsonProvider, final List<JsonNode> values) {
		final JsonNode result = jsonProvider.createArray();
		for (final JsonNode value : values)
			jsonProvider.add(result, value);
		return result;
	}

	public static <JsonNode> List<JsonNode> asArrayList(final JsonProvider<JsonNode> jsonProvider, final JsonNode in) {
		return Lists.newArrayList(jsonProvider.elements(in));
	}

	public static <JsonNode> String typeOf(final JsonProvider<JsonNode> jsonProvider, final JsonNode in) {
		if (in == null)
			return "null";
		switch (jsonProvider.getNodeType(in)) {
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
				throw new IllegalArgumentException("Unknown JsonNodeType: " + jsonProvider.getNodeType(in));
		}
	}

	public static <JsonNode> JsonNode nullToNullNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode value) {
		if (value == null)
			return jsonProvider.createNull();
		return value;
	}

	private static <JsonNode> JsonNode filterInternal(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Predicate<JsonNode> pred) {
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			final JsonNode out = jsonProvider.createObject();
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				if (!pred.test(entry.getValue()))
					continue;
				jsonProvider.set(out, entry.getKey(), filterInternal(jsonProvider, entry.getValue(), pred));
			}
			return out;
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			final JsonNode out = jsonProvider.createArray();
			final Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				final JsonNode val = iter.next();
				if (!pred.test(val))
					continue;
				jsonProvider.add(out, filterInternal(jsonProvider, val, pred));
			}
			return out;
		} else {
			return in;
		}
	}

	public static <JsonNode> JsonNode filter(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Predicate<JsonNode> pred) {
		if (!pred.test(in))
			return jsonProvider.createNull();
		return filterInternal(jsonProvider, in, pred);
	}

	public static <JsonNode> String toString(final JsonProvider<JsonNode> jsonProvider, final JsonNode node) {
		return jsonProvider.toString(node);
	}

	/**
	 * Returns true if the node is a value node (not a container node like array or object).
	 */
	public static <JsonNode> boolean isValueNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode node) {
		final JsonNodeType type = jsonProvider.getNodeType(node);
		return type != JsonNodeType.ARRAY && type != JsonNodeType.OBJECT;
	}
}
