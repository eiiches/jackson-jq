package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonPredicateFunction;

@BuiltinFunction("isinfinite/0")
public class IsInfiniteFunction extends JsonPredicateFunction {
	public IsInfiniteFunction() {
		super(IsInfiniteFunction::test);
	}

	private static boolean test(final JsonNode value) {
		if ((value.isDouble() || value.isFloat()) && Double.isInfinite(value.asDouble()))
			return true;
		return false;
	}
}
