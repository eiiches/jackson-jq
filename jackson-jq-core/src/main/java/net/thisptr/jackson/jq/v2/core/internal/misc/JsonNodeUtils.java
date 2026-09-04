package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class JsonNodeUtils {
	private JsonNodeUtils() {
	}

	public static <JsonNode> boolean asBoolean(JsonProvider<JsonNode> jsonProvider, JsonNode n) {
		if (n == null || jsonProvider.isNull(n))
			return false;
		if (jsonProvider.isBoolean(n))
			return jsonProvider.getBoolean(n);
		return true;
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, long value) {
		if (((int) value) == value)
			return jsonProvider.createNumber((int) value);
		return jsonProvider.createNumber(value);
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, double value) {
		if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE && value == Math.rint(value))
			return jsonProvider.createNumber((int) value);
		if (value >= Long.MIN_VALUE && value < 0x1p63 && value == Math.rint(value))
			return asNumericNode(jsonProvider, (long) value);
		return jsonProvider.createNumber(value);
	}

	public static <JsonNode> JsonNode asArrayNode(JsonProvider<JsonNode> jsonProvider, List<JsonNode> values) {
		return jsonProvider.createArray(values);
	}

	public static <JsonNode> List<JsonNode> asArrayList(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return Lists.newArrayList(jsonProvider.getArrayElements(in));
	}

	public static <JsonNode> String typeOf(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return jsonProvider.getNodeType(in).toString().toLowerCase(Locale.ROOT);
	}

	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		return jsonProvider.format(node);
	}

	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node, @Nullable Version version) {
		String text = jsonProvider.format(node);
		if (version != null && version.compareTo(Versions.JQ_1_7) < 0) {
			if (jsonProvider.isNumber(node)) {
				return text.replace('E', 'e');
			}
		}
		return text;
	}

	/**
	 * Returns true if the node is a value node (not a container node like array or object).
	 */
	public static <JsonNode> boolean isValueNode(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		return type != JsonNodeType.ARRAY && type != JsonNodeType.OBJECT;
	}

}
