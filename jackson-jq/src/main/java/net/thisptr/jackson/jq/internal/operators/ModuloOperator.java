package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ModuloOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		final JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			final double lhsDouble = jsonProvider.asDouble(lhs);
			final double rhsDouble = jsonProvider.asDouble(rhs);

			// Handle Infinity: convert to long representation
			final long dividend = Double.isNaN(lhsDouble) ? 0L
				: Double.isInfinite(lhsDouble) ? (lhsDouble > 0 ? Long.MAX_VALUE : Long.MIN_VALUE)
				: (long) lhsDouble;

			// If divisor is NaN, return the dividend (jq 1.5 behavior)
			if (Double.isNaN(rhsDouble))
				return JsonNodeUtils.asNumericNode(jsonProvider, dividend);

			final long divisor = Double.isInfinite(rhsDouble)
				? (rhsDouble > 0 ? Long.MAX_VALUE : Long.MIN_VALUE)
				: (long) rhsDouble;

			if (divisor == 0L)
				throw new JsonQueryException(jsonProvider, "%s and %s cannot be divided (remainder) because the divisor is zero", lhs, rhs);
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend % divisor);
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be divided (remainder)", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "%";
	}
}
