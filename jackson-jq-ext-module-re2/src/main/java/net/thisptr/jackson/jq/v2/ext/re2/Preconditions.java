package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.Arrays;
import java.util.Locale;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class Preconditions {
	private Preconditions() {
	}

	static <JsonNode> void checkInputType(JsonProvider<JsonNode> jsonProvider, String functionName, JsonNode input, JsonNodeType... types) throws JsonQueryException {
		JsonNodeType actual = jsonProvider.getNodeType(input);
		for (JsonNodeType type : types)
			if (actual == type)
				return;
		throw new JsonQueryException(String.format("%s is not applicable to %s; expected one of %s", functionName, actual, Arrays.toString(types)));
	}

	static <JsonNode> void checkArgumentType(JsonProvider<JsonNode> jsonProvider, String functionName, int argumentIndex, JsonNode value, JsonNodeType... types) throws JsonQueryException {
		JsonNodeType actual = jsonProvider.getNodeType(value);
		for (JsonNodeType type : types)
			if (actual == type)
				return;

		String indexText;
		switch (argumentIndex) {
			case 1:
				indexText = "1st";
				break;
			case 2:
				indexText = "2nd";
				break;
			case 3:
				indexText = "3rd";
				break;
			default:
				indexText = argumentIndex + "th";
		}
		throw new JsonQueryException(String.format("cannot accept %s as %s argument of %s; expected one of %s", actual, indexText, functionName, Arrays.toString(types)));
	}

	static <JsonNode> JsonQueryException cannotBeAdded(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode left, JsonNode right) {
		return new JsonQueryException(String.format("%s and %s cannot be added", describe(jsonProvider, version, left), describe(jsonProvider, version, right)));
	}

	private static <JsonNode> String describe(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode node) {
		return String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(Locale.ROOT), truncate(jsonProvider.format(node), version));
	}

	private static String truncate(String text, Version version) {
		if (version.compareTo(Version.of(1, 8, 2)) >= 0) {
			if (text.length() <= 29)
				return text;
			@Var char delimiter = 0;
			if (text.startsWith("\"")) delimiter = '"';
			else if (text.startsWith("[")) delimiter = ']';
			else if (text.startsWith("{")) delimiter = '}';
			int length = delimiter != 0 ? 25 : 26;
			return text.substring(0, length) + "..." + (delimiter != 0 ? delimiter : "");
		}
		if (text.length() <= 14)
			return text;
		return text.substring(0, 11) + "...";
	}
}
