package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonPredicateFunction;

@AutoService(Function.class)
@BuiltinFunction("isinfinite/0")
public class IsInfiniteFunction extends JsonPredicateFunction {
	public IsInfiniteFunction() {
		super(IsInfiniteFunction::test);
	}

	private static boolean test(final JsonNode value) {
		return (value.isDouble() || value.isFloat()) && Double.isInfinite(value.asDouble());
	}
}
