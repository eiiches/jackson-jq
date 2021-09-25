package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.JsonPredicateFunction;

@AutoService(Function.class)
@BuiltinFunction("isnan/0")
public class IsNanFunction extends JsonPredicateFunction {
	public IsNanFunction() {
		super(IsNanFunction::test);
	}

	private static boolean test(final JsonNode value) {
		return (value.isDouble() || value.isFloat()) && Double.isNaN(value.asDouble());
	}
}
