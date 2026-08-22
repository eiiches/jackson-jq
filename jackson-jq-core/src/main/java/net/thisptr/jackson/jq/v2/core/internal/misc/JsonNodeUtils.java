package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;

public class JsonNodeUtils {
	private JsonNodeUtils() {}

	public static <JsonNode> boolean asBoolean(JsonProvider<JsonNode> jsonProvider, JsonNode n) {
		if (n == null || jsonProvider.getNodeType(n) == JsonNodeType.NULL)
			return false;
		if (jsonProvider.getNodeType(n) == JsonNodeType.BOOLEAN)
			return jsonProvider.asBoolean(n);
		return true;
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, long value) {
		if (((int) value) == value)
			return jsonProvider.createNumber((int) value);
		return jsonProvider.createNumber(value);
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, double value) {
		if (((int) value) == value)
			return jsonProvider.createNumber((int) value);
		if (((long) value) == value)
			return asNumericNode(jsonProvider, (long) value);
		return jsonProvider.createNumber(value);
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
		return jsonProvider.getNodeType(in).toString().toLowerCase(Locale.ROOT);
	}

	public static <JsonNode> String formatType(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		JsonNodeType type = jsonProvider.getNodeType(in);
		switch (type) {
			case NULL:
				return "null";
			case BOOLEAN:
				return jsonProvider.asBoolean(in) ? "true" : "false";
			case NUMBER:
				return jsonProvider.toString(in);
			case STRING:
				return String.format("\"%s\"", jsonProvider.asText(in));
			case ARRAY:
				return "array";
			case OBJECT:
				return "object";
			default:
				throw new IllegalStateException("Unknown type: " + type);
		}
	}

	public static <JsonNode> String formatTypes(JsonProvider<JsonNode> jsonProvider, List<JsonNode> in) {
		StringBuilder sb = new StringBuilder();
		for (JsonNode n : in) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(formatType(jsonProvider, n));
		}
		return sb.toString();
	}

	public static <JsonNode> String print(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return jsonProvider.toString(in);
	}

	public static <JsonNode> boolean isIterable(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		JsonNodeType type = jsonProvider.getNodeType(in);
		return type == JsonNodeType.ARRAY || type == JsonNodeType.OBJECT;
	}

	public static <JsonNode> JsonNode nullToNullNode(JsonProvider<JsonNode> jsonProvider, @Nullable JsonNode value) {
		if (value == null)
			return jsonProvider.createNull();
		return value;
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

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, JsonNode in, JsonNode accessor) {
		String inType = jsonProvider.getNodeType(in).toString().toLowerCase(Locale.ROOT);
		return cannotIndex(jsonProvider, version, inType, accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, JsonNodeType inType, JsonNode accessor) {
		return cannotIndex(jsonProvider, version, inType.toString().toLowerCase(Locale.ROOT), accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, String inType, JsonNode accessor) {
		JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
		if (version != null && version.compareTo(Version.valueOf(1, 8, 2)) >= 0) {
			String formatted = Strings.truncate(jsonProvider.toString(accessor), version);
			return String.format("Cannot index %s with %s (%s)", inType, accessorType.toString().toLowerCase(Locale.ROOT), formatted);
		} else {
			if (accessorType == JsonNodeType.STRING) {
				return String.format("Cannot index %s with string \"%s\"", inType, jsonProvider.asText(accessor));
			} else {
				return String.format("Cannot index %s with %s", inType, accessorType.toString().toLowerCase(Locale.ROOT));
			}
		}
	}
}
