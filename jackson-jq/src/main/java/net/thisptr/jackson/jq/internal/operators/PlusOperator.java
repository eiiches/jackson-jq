package net.thisptr.jackson.jq.internal.operators;

import java.util.Map.Entry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

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
		} else if (lhs.isString() && rhs.isString()) {
			return new StringNode(lhs.asString() + rhs.asString());
		} else if (lhs.isObject() && rhs.isObject()) {
			final ObjectNode result = mapper.createObjectNode();
			for (final Entry<String, JsonNode> e : lhs.properties()) {
				result.set(e.getKey(), e.getValue());
			}
			for (final Entry<String, JsonNode> e : rhs.properties()) {
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
