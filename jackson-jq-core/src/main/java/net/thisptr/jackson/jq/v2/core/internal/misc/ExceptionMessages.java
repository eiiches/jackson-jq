package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.Locale;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;

public final class ExceptionMessages {
	private ExceptionMessages() {
	}

	public static String truncate(String text, @Nullable Version version) {
		if (version != null && version.compareTo(Version.of(1, 8, 2)) >= 0) {
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

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, JsonNode in, JsonNode accessor) {
		String inType = jsonProvider.getNodeType(in).toString().toLowerCase(Locale.ROOT);
		return cannotIndex(jsonProvider, version, inType, accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, JsonNodeType inType, JsonNode accessor) {
		return cannotIndex(jsonProvider, version, inType.toString().toLowerCase(Locale.ROOT), accessor);
	}

	public static <JsonNode> String cannotIndex(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, String inType, JsonNode accessor) {
		JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
		if (version != null && version.compareTo(Version.of(1, 8, 2)) >= 0) {
			String formatted = truncate(jsonProvider.format(accessor), version);
			return String.format("Cannot index %s with %s (%s)", inType, accessorType.toString().toLowerCase(Locale.ROOT), formatted);
		} else {
			if (accessorType == JsonNodeType.STRING) {
				return String.format("Cannot index %s with string \"%s\"", inType, jsonProvider.getString(accessor));
			} else {
				return String.format("Cannot index %s with %s", inType, accessorType.toString().toLowerCase(Locale.ROOT));
			}
		}
	}

	@FormatMethod
	public static <JsonNode> String format(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, @FormatString String format, Object... args) {
		Object[] formattedArguments = new Object[args.length];
		for (int i = 0; i < args.length; ++i) {
			if (jsonProvider.isJsonNodeInstance(args[i])) {
				@SuppressWarnings("unchecked") JsonNode node = (JsonNode) args[i];
				@Var String json;
				try {
					json = truncate(jsonProvider.format(node), version);
				} catch (Exception e) {
					json = "<failed to format json>";
				}
				formattedArguments[i] = String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(Locale.ROOT), json);
			} else if (args[i] instanceof JsonNodeType) {
				JsonNodeType type = (JsonNodeType) args[i];
				formattedArguments[i] = type.toString().toLowerCase(Locale.ROOT);
			} else {
				formattedArguments[i] = args[i];
			}
		}
		return String.format(format, formattedArguments);
	}
}
