package net.thisptr.jackson.jq.internal.misc;

import java.util.Arrays;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

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
}
