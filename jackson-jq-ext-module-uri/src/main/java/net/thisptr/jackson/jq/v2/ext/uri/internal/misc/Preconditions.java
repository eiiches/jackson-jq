package net.thisptr.jackson.jq.v2.ext.uri.internal.misc;

import java.util.Arrays;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class Preconditions {
	public static <JsonNode> void checkInputType(JsonProvider<JsonNode> jsonProvider, String fname, JsonNode in, JsonNodeType... types) throws JsonQueryException {
		JsonNodeType t = jsonProvider.getNodeType(in);
		for (JsonNodeType type : types)
			if (t == type)
				return;
		throw new JsonQueryException(String.format("%s is not applicable to %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}
}
