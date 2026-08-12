package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class JsonNodeUtils {
	private JsonNodeUtils() {}

	public static <JsonNode> boolean asBoolean(JsonProvider<JsonNode> jsonProvider, JsonNode n) {
		if (n == null || jsonProvider.getNodeType(n) == JsonNodeType.NULL || jsonProvider.isMissingNode(n))
			return false;
		if (jsonProvider.getNodeType(n) == JsonNodeType.BOOLEAN)
			return jsonProvider.asBoolean(n);
		return true;
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, long value) {
		if (((int) value) == value)
			return jsonProvider.createInt((int) value);
		return jsonProvider.createLong(value);
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, double value) {
		if (((int) value) == value)
			return jsonProvider.createInt((int) value);
		if (((long) value) == value)
			return jsonProvider.createLong((long) value);
		return jsonProvider.createDouble(value);
	}

	public static <JsonNode> JsonNode asArrayNode(JsonProvider<JsonNode> jsonProvider, List<JsonNode> values) {
		JsonNode result = jsonProvider.createArray();
		for (JsonNode value : values)
			jsonProvider.add(result, value);
		return result;
	}

	public static <JsonNode> List<JsonNode> asArrayList(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return Lists.newArrayList(jsonProvider.elements(in));
	}

	public static <JsonNode> String typeOf(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
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

	public static <JsonNode> JsonNode nullToNullNode(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		if (value == null)
			return jsonProvider.createNull();
		return value;
	}

	private static <JsonNode> JsonNode filterInternal(JsonProvider<JsonNode> jsonProvider, JsonNode in, Predicate<JsonNode> pred) {
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			JsonNode out = jsonProvider.createObject();
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				if (!pred.test(entry.getValue()))
					continue;
				jsonProvider.set(out, entry.getKey(), filterInternal(jsonProvider, entry.getValue(), pred));
			}
			return out;
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			JsonNode out = jsonProvider.createArray();
			Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				JsonNode val = iter.next();
				if (!pred.test(val))
					continue;
				jsonProvider.add(out, filterInternal(jsonProvider, val, pred));
			}
			return out;
		} else {
			return in;
		}
	}

	public static <JsonNode> JsonNode filter(JsonProvider<JsonNode> jsonProvider, JsonNode in, Predicate<JsonNode> pred) {
		if (!pred.test(in))
			return jsonProvider.createNull();
		return filterInternal(jsonProvider, in, pred);
	}

	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		return jsonProvider.toString(node);
	}

	/**
	 * Returns true if the node is a value node (not a container node like array or object).
	 */
	public static <JsonNode> boolean isValueNode(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		return type != JsonNodeType.ARRAY && type != JsonNodeType.OBJECT;
	}
}
