package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonPredicateFunction;

@BuiltinFunction("isnan/0")
public class IsNanFunction extends JsonPredicateFunction {
	public IsNanFunction() {
		super(IsNanFunction::test);
	}

	private static boolean test(final JsonNode value) {
		if ((value.isDouble() || value.isFloat()) && Double.isNaN(value.asDouble()))
			return true;
		return false;
	}
}
