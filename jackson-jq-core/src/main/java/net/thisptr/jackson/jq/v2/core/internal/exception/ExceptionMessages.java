package net.thisptr.jackson.jq.v2.core.internal.exception;

import java.util.Locale;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class ExceptionMessages {
	/**
	 * The buffer jq gives the value it describes in a type error, and the key it names in an invalid
	 * path expression.
	 */
	public static final int SHORT_BUFFER_SIZE = 15;

	/**
	 * The buffer jq gives the value it reports in an invalid path expression.
	 */
	public static final int LONG_BUFFER_SIZE = 30;

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
			json = truncate(JsonCodec.format(jsonProvider, node), version, SHORT_BUFFER_SIZE);
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

	/**
	 * Truncates rendered JSON the way jq truncates it when it copies a value into a fixed buffer to
	 * name it in an error message.
	 * <p>
	 * jq keeps what fits in the buffer and overwrites the last three characters with an ellipsis, so the
	 * result can end mid-string. jq 1.8.2 truncates one character earlier and closes the {@code "},
	 * {@code ]} or {@code }} it cut through, and uses {@link #LONG_BUFFER_SIZE} everywhere, dropping the
	 * distinction between the two buffers.
	 *
	 * @param text the rendered JSON
	 * @param version the jq compatibility version, which selects the truncation rule
	 * @param bufferSize the size of the buffer jq would copy {@code text} into
	 * @return the truncated text
	 */
	public static String truncate(String text, Version version, int bufferSize) {
		if (version.compareTo(Version.of(1, 8, 2)) >= 0) {
			if (text.length() <= LONG_BUFFER_SIZE - 1)
				return text;
			@Var char delim = 0;
			if (text.startsWith("\"")) delim = '"';
			else if (text.startsWith("[")) delim = ']';
			else if (text.startsWith("{")) delim = '}';
			int l = delim != 0 ? LONG_BUFFER_SIZE - 5 : LONG_BUFFER_SIZE - 4;
			return text.substring(0, l) + "..." + (delim != 0 ? delim : "");
		} else {
			if (text.length() <= bufferSize - 1)
				return text;
			return text.substring(0, bufferSize - 4) + "...";
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
			String formatted = truncate(JsonCodec.format(jsonProvider, accessor), version, SHORT_BUFFER_SIZE);
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
