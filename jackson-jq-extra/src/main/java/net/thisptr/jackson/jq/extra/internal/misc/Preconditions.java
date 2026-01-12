package net.thisptr.jackson.jq.extra.internal.misc;

import java.util.Arrays;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;

public class Preconditions {

	public static <JsonNode> void checkInputType(final JsonProvider<JsonNode> jsonProvider, final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = jsonProvider.getNodeType(in);
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}
}
