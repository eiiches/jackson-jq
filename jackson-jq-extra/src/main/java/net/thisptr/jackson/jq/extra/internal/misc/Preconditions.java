package net.thisptr.jackson.jq.extra.internal.misc;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.exception.IllegalJsonInputException;

public class Preconditions {

	public static void checkInputType(final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = in.getNodeType();
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to %s; expected one of %s", fname, in.getNodeType(), Arrays.toString(types)));
	}
}
