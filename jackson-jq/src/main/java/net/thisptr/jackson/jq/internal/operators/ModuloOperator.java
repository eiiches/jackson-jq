package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModuloOperator implements BinaryOperator {

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isIntegralNumber() && rhs.isIntegralNumber()) {
			final long r = lhs.asLong() % rhs.asLong();
			return JsonNodeUtils.asNumericNode(r);
		} else {
			throw new JsonQueryException("to calculate module, both sides must be a integer");
		}
	}

	@Override
	public String image() {
		return "%";
	}
}
