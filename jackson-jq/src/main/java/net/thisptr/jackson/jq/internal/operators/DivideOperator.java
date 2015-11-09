package net.thisptr.jackson.jq.internal.operators;

import java.util.regex.Pattern;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class DivideOperator implements BinaryOperator {

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isNumber() && rhs.isNumber()) {
			final double r = lhs.asDouble() / rhs.asDouble();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isTextual() && rhs.isTextual()) {
			final ArrayNode result = mapper.createArrayNode();
			for (final String token : lhs.asText().split(Pattern.quote(rhs.asText())))
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
