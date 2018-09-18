package net.thisptr.jackson.jq.internal.operators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class DivideOperator implements BinaryOperator {

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isNumber() && rhs.isNumber()) {
			final double divisor = rhs.asDouble();
			final double dividend = lhs.asDouble();
			if (divisor == 0.0)
				throw JsonQueryException.format("number (%s) and number (%s) cannot be divided because the divisor is zero", dividend, divisor);
			return JsonNodeUtils.asNumericNode(dividend / divisor);
		} else if (lhs.isTextual() && rhs.isTextual()) {
			final ArrayNode result = mapper.createArrayNode();
			for (final String token : Strings.split(lhs.asText(), rhs.asText()))
				result.add(new TextNode(token));
			return result;
		} else {
			throw new JsonQueryTypeException(lhs, rhs, "cannot be divided");
		}
	}

	@Override
	public String image() {
		return "/";
	}
}
