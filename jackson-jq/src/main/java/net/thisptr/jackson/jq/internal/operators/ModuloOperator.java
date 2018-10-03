package net.thisptr.jackson.jq.internal.operators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ModuloOperator implements BinaryOperator {
	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isNumber() && rhs.isNumber()) {
			final long divisor = rhs.asLong();
			final long dividend = lhs.asLong();
			if (Double.isNaN(rhs.asDouble()))
				return JsonNodeUtils.asNumericNode(dividend);
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
