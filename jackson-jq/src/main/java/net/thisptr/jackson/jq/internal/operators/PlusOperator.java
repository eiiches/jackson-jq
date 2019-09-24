package net.thisptr.jackson.jq.internal.operators;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class PlusOperator implements BinaryOperator {
	@Override
	public JsonNode apply(final ObjectMapper mapper, final JsonNode lhs, final JsonNode rhs) throws JsonQueryException {
		if (lhs.isIntegralNumber() && rhs.isIntegralNumber()) {
			final long r = lhs.asLong() + rhs.asLong();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isNumber() && rhs.isNumber()) {
			final double r = lhs.asDouble() + rhs.asDouble();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isArray() && rhs.isArray()) {
			final ArrayNode result = mapper.createArrayNode();
			result.addAll((ArrayNode) lhs);
			result.addAll((ArrayNode) rhs);
			return result;
		} else if (lhs.isTextual() && rhs.isTextual()) {
			return new TextNode(lhs.asText() + rhs.asText());
		} else if (lhs.isObject() && rhs.isObject()) {
			final ObjectNode result = mapper.createObjectNode();
			final Iterator<Entry<String, JsonNode>> liter = lhs.fields();
			while (liter.hasNext()) {
				final Entry<String, JsonNode> e = liter.next();
				result.set(e.getKey(), e.getValue());
			}
			final Iterator<Entry<String, JsonNode>> riter = rhs.fields();
			while (riter.hasNext()) {
				final Entry<String, JsonNode> e = riter.next();
				result.set(e.getKey(), e.getValue());
			}
			return result;
		} else if (lhs.isNull()) {
			return rhs;
		} else if (rhs.isNull()) {
			return lhs;
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be added", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "+";
	}
}
