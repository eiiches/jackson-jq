package net.thisptr.jackson.jq.v2.ext.time.internal.misc;

import java.util.Arrays;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class Preconditions {
	public static <JsonNode> void checkInputType(final JsonProvider<JsonNode> jsonProvider, final String fname, final JsonNode in, final JsonNodeType... types) throws JsonQueryException {
		final JsonNodeType t = jsonProvider.getNodeType(in);
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new JsonQueryException(String.format("%s is not applicable to %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}
}
