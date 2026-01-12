package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class DivideOperator<JsonNode> implements BinaryOperator<JsonNode> {

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		final JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			final double divisor = jsonProvider.asDouble(rhs);
			final double dividend = jsonProvider.asDouble(lhs);
			if (divisor == 0.0)
				throw new JsonQueryException(jsonProvider, "%s and %s cannot be divided because the divisor is zero", lhs, rhs);
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend / divisor);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			final JsonNode result = jsonProvider.createArray();
			for (final String token : Strings.split(jsonProvider.asText(lhs), jsonProvider.asText(rhs)))
				jsonProvider.add(result, jsonProvider.createString(token));
			return result;
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be divided", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "/";
	}
}
