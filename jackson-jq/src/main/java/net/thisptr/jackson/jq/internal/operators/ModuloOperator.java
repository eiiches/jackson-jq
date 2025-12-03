package net.thisptr.jackson.jq.internal.operators;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ModuloOperator implements BinaryOperator {
	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isNumber() && rhs.isNumber()) {
			// Check for NaN before calling asLong() as Jackson 3.x throws on NaN
			final double rhsDouble = rhs.asDouble();
			final double lhsDouble = lhs.asDouble();
			if (Double.isNaN(rhsDouble)) {
				return JsonNodeUtils.asNumericNode((long) lhsDouble);
			}
			final long divisor = (long) rhsDouble;
			final long dividend = (long) lhsDouble;
			if (divisor == 0L)
				throw new JsonQueryException("%s and %s cannot be divided (remainder) because the divisor is zero", lhs, rhs);
			return JsonNodeUtils.asNumericNode(dividend % divisor);
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be divided (remainder)", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "%";
	}
}
