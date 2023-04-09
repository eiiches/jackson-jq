package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.JsonPredicateFunction;

@AutoService(Function.class)
@BuiltinFunction("isnormal/0")
public class IsNormalFunction extends JsonPredicateFunction {
	public IsNormalFunction() {
		super(IsNormalFunction::test);
	}

	private static boolean test(final JsonNode value) {
		if (!value.isNumber())
			return false;
		final double v = value.asDouble();
		return !Double.isInfinite(v) && (v <= -Double.MIN_NORMAL || Double.MIN_NORMAL <= v);
	}
}
