package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.Arrays;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
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
}
