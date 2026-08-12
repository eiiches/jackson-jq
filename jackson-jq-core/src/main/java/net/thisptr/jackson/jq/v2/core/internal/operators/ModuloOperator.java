package net.thisptr.jackson.jq.v2.core.internal.operators;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class ModuloOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double lhsDouble = jsonProvider.asDouble(lhs);
			double rhsDouble = jsonProvider.asDouble(rhs);

			// Handle Infinity: convert to long representation
			long dividend = Double.isNaN(lhsDouble) ? 0L
				: Double.isInfinite(lhsDouble) ? (lhsDouble > 0 ? Long.MAX_VALUE : Long.MIN_VALUE)
				: (long) lhsDouble;

			// If divisor is NaN, return the dividend (jq 1.5 behavior)
			if (Double.isNaN(rhsDouble))
				return JsonNodeUtils.asNumericNode(jsonProvider, dividend);

			long divisor = Double.isInfinite(rhsDouble)
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
