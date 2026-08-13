package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.v2.core.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class Preconditions {

	public static <JsonNode> void checkArgumentCount(String fname, List<Expression> args, int... nums) throws IllegalJsonArgumentException {
		int nargs = args.size();
		for (int num : nums)
			if (nargs == num)
				return;
		throw new IllegalJsonArgumentException(String.format("%s takes %s arguments; got %s", fname, Arrays.toString(nums), nargs));
	}

	public static <JsonNode> void checkInputType(JsonProvider<JsonNode> jsonProvider, String fname, JsonNode in, JsonNodeType... types) throws IllegalJsonInputException {
		JsonNodeType t = jsonProvider.getNodeType(in);
		for (JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}

	private static <JsonNode> void checkInputElementType(JsonProvider<JsonNode> jsonProvider, String fname, JsonNode in, JsonNodeType... types) throws IllegalJsonInputException {
		JsonNodeType t = jsonProvider.getNodeType(in);
		for (JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to input which contains %s; expected one of %s", fname, jsonProvider.getNodeType(in), Arrays.toString(types)));
	}

	public static <JsonNode> void checkInputArrayType(JsonProvider<JsonNode> jsonProvider, String fname, JsonNode in, JsonNodeType... types) throws IllegalJsonInputException {
		checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext())
			checkInputElementType(jsonProvider, fname, iter.next(), types);
	}

	public static <JsonNode> void checkArgumentType(JsonProvider<JsonNode> jsonProvider, String fname, int aindex, JsonNode value, JsonNodeType... types) throws IllegalJsonArgumentException {
		JsonNodeType t = jsonProvider.getNodeType(value);
		for (JsonNodeType type : types)
			if (t == type)
				return;

		String indexText;
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
