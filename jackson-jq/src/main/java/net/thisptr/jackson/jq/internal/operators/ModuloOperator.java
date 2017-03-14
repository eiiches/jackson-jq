package net.thisptr.jackson.jq.internal.operators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ModuloOperator implements BinaryOperator {
	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isIntegralNumber() && rhs.isIntegralNumber()) {
			final long divisor = rhs.asLong();
			final long dividend = lhs.asLong();
			if (divisor == 0L)
				throw JsonQueryException.format("number (%s) and number (%s) cannot be divided (remainder) because the divisor is zero", dividend, divisor);
			return JsonNodeUtils.asNumericNode(dividend % divisor);
		} else {
			throw new JsonQueryException("to calculate module, both sides must be a integer");
		}
	}

	@Override
	public String image() {
		return "%";
	}
}
