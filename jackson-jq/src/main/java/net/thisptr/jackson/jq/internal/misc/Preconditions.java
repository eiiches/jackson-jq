package net.thisptr.jackson.jq.internal.misc;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;

public class Preconditions {

	public static void checkArgumentCount(final String fname, final List<JsonQuery> args, final int... nums) throws IllegalJsonArgumentException {
		final int nargs = args.size();
		for (final int num : nums)
			if (nargs == num)
				return;
		throw new IllegalJsonArgumentException(String.format("%s takes %s arguments; got %s", fname, Arrays.toString(nums), nargs));
	}

	public static void checkInputType(final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = in.getNodeType();
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to %s; expected one of %s", fname, in.getNodeType(), Arrays.toString(types)));
	}

	private static void checkInputElementType(final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		final JsonNodeType t = in.getNodeType();
		for (final JsonNodeType type : types)
			if (t == type)
				return;
		throw new IllegalJsonInputException(String.format("%s is not applicable to input which contains %s; expected one of %s", fname, in.getNodeType(), Arrays.toString(types)));
	}

	public static void checkInputArrayType(final String fname, final JsonNode in, final JsonNodeType... types) throws IllegalJsonInputException {
		checkInputType(fname, in, JsonNodeType.ARRAY);
		for (final JsonNode i : in)
			checkInputElementType(fname, i, types);
	}

	public static void checkArgumentType(final String fname, final int aindex, final JsonNode value, final JsonNodeType... types) throws IllegalJsonArgumentException {
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
		throw new IllegalJsonArgumentException(String.format("cannot accept %s as %s argument of %s; expected one of %s", value.getNodeType(), indexText, fname, Arrays.toString(types)));
	}
}
