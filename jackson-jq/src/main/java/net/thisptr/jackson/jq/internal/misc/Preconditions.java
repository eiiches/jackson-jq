package net.thisptr.jackson.jq.internal.misc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;

public class Preconditions {

	public static <JsonNode> void checkArgumentCount(final String fname, final List<Expression<JsonNode>> args, final int... nums) throws IllegalJsonArgumentException {
		final int nargs = args.size();
		for (final int num : nums)
			if (nargs == num)
				return;
		throw new IllegalJsonArgumentException(String.format("%s takes %s arguments; got %s", fname, Arrays.toString(nums), nargs));
	}

	public static <JsonNode> void checkInputType(final JsonProvider<JsonNode> jsonProvider, final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = jsonProvider.getNodeType(in);
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}

	private static <JsonNode> void checkInputElementType(final JsonProvider<JsonNode> jsonProvider, final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = jsonProvider.getNodeType(in);
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to input which contains %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}

	public static <JsonNode> void checkInputArrayType(final JsonProvider<JsonNode> jsonProvider, final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext())
			checkInputElementType(jsonProvider, fname, iter.next(), types);
	}

	public static <JsonNode> void checkArgumentType(final JsonProvider<JsonNode> jsonProvider, final String fname, final int aindex, final JsonNode value, final JsonNodeType... types) throws IllegalJsonArgumentException {
		final JsonNodeType t = jsonProvider.getNodeType(value);
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
		throw new IllegalJsonArgumentException(String.format("cannot accept %s as %s argument of %s; expected one of %s", jsonProvider.getNodeType(value), indexText, fname, Arrays.toString(types)));
	}
}
