package net.thisptr.jackson.jq.v2.core.internal.exception;

import java.util.Locale;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class ExceptionMessages {
	private ExceptionMessages() {
	}

	/**
	 * Renders a JSON node as {@code <type> (<truncated json>)}.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider that owns {@code node}
	 * @param version the jq compatibility version, which selects the truncation rule
	 * @param node the JSON node to render
	 * @return the rendered description
	 */
	public static <JsonNode> String describe(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode node) {
		@Var String json;
		try {
			json = truncate(jsonProvider.format(node), version);
		} catch (Exception e) {
			json = "<failed to format json>";
		}
		return String.format("%s (%s)", typeName(jsonProvider.getNodeType(node)), json);
	}

	/**
	 * Renders a node type as the lowercase name jq uses in error messages.
	 *
	 * @param type the node type
	 * @return the lowercase type name
	 */
	public static String typeName(JsonNodeType type) {
		return type.toString().toLowerCase(Locale.ROOT);
	}

	public static String truncate(String text, Version version) {
		if (version.compareTo(Version.of(1, 8, 2)) >= 0) {
			if (text.length() <= 29)
				return text;
			@Var char delim = 0;
			if (text.startsWith("\"")) delim = '"';
			else if (text.startsWith("[")) delim = ']';
			else if (text.startsWith("{")) delim = '}';
			int l = delim != 0 ? 25 : 26;
			return text.substring(0, l) + "..." + (delim != 0 ? delim : "");
		} else {
			if (text.length() <= 14)
				return text;
			return text.substring(0, 11) + "...";
		}
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode in, JsonNode accessor) {
		return cannotIndex(jsonProvider, version, typeName(jsonProvider.getNodeType(in)), accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, Version version, JsonNodeType inType, JsonNode accessor) {
		return cannotIndex(jsonProvider, version, typeName(inType), accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, Version version, String inType, JsonNode accessor) {
		JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
		if (version.compareTo(Version.of(1, 8, 2)) >= 0) {
			String formatted = truncate(jsonProvider.format(accessor), version);
			return String.format("Cannot index %s with %s (%s)", inType, typeName(accessorType), formatted);
		} else {
			if (accessorType == JsonNodeType.STRING) {
				return String.format("Cannot index %s with string \"%s\"", inType, jsonProvider.getString(accessor));
			} else {
				return String.format("Cannot index %s with %s", inType, typeName(accessorType));
			}
		}
	}

	public static String invalidSliceBounds(Version version, JsonNodeType inType) {
		if (version.compareTo(Version.of(1, 7)) >= 0)
			return "Array/string slice indices must be integers";
		return String.format("Start and end indices of an %s slice must be numbers", typeName(inType));
	}
}
