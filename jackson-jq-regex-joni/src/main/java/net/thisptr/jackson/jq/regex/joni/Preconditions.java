package net.thisptr.jackson.jq.regex.joni;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.exception.JsonQueryException;

final class Preconditions {
	private Preconditions() {}

	static void checkInputType(final String fname, final JsonNode in, final JsonNodeType... types) throws JsonQueryException {
		final JsonNodeType t = in.getNodeType();
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new JsonQueryException(String.format("%s is not applicable to %s; expected one of %s", fname, in.getNodeType(), Arrays.toString(types)));
	}

	static void checkArgumentType(final String fname, final int aindex, final JsonNode value, final JsonNodeType... types) throws JsonQueryException {
		final JsonNodeType t = value.getNodeType();
		for (final JsonNodeType type : types)
			if (t == type)
				return;

		final String indexText;
		switch (aindex) {
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
				indexText = aindex + "th";
		}
		throw new JsonQueryException(String.format("cannot accept %s as %s argument of %s; expected one of %s", value.getNodeType(), indexText, fname, Arrays.toString(types)));
	}
}
