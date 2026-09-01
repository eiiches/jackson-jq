package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.Arrays;
import java.util.Locale;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

final class Preconditions {
	private Preconditions() {
	}

	static <JsonNode> void checkInputType(JsonProvider<JsonNode> jsonProvider, String fname, JsonNode in, JsonNodeType... types) throws JsonQueryException {
		JsonNodeType actual = jsonProvider.getNodeType(in);
		for (JsonNodeType type : types)
			if (actual == type)
				return;
		throw new JsonQueryException(String.format("%s is not applicable to %s; expected one of %s", fname, actual, Arrays.toString(types)));
	}

	static <JsonNode> void checkArgumentType(JsonProvider<JsonNode> jsonProvider, String fname, int argumentIndex, JsonNode value, JsonNodeType... types) throws JsonQueryException {
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
		throw new JsonQueryException(String.format("cannot accept %s as %s argument of %s; expected one of %s", actual, indexText, fname, Arrays.toString(types)));
	}

	/**
	 * Builds jq's error for a string concatenation that cannot be performed, e.g.
	 * {@code string ("a") and number (1) cannot be added}.
	 * <p>
	 * This duplicates the node rendering of {@code ExceptionMessages} in jackson-jq-core, which
	 * this module deliberately does not depend on at compile scope.
	 */
	static <JsonNode> JsonQueryException cannotBeAdded(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode lhs, JsonNode rhs) {
		return new JsonQueryException(String.format("%s and %s cannot be added", describe(jsonProvider, version, lhs), describe(jsonProvider, version, rhs)));
	}

	private static <JsonNode> String describe(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode node) {
		return String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(Locale.ROOT), truncate(jsonProvider.format(node), version));
	}

	private static String truncate(String text, Version version) {
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
}
