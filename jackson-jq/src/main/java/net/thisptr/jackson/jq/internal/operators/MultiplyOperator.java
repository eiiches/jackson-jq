package net.thisptr.jackson.jq.internal.operators;

import java.util.Map.Entry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class MultiplyOperator implements BinaryOperator {
	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isIntegralNumber() && rhs.isIntegralNumber()) {
			final long r = lhs.asLong() * rhs.asLong();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isNumber() && rhs.isNumber()) {
			final double r = lhs.asDouble() * rhs.asDouble();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isString() && rhs.isNumber()) {
			final double count = rhs.asDouble();
			if (count <= 0)
				return NullNode.getInstance();
			if (count < 2)
				return lhs;
			return new StringNode(Strings.repeat(lhs.asString(), (int) count));
		} else if (lhs.isNumber() && rhs.isString()) {
			final double count = lhs.asDouble();
			if (count <= 0)
				return NullNode.getInstance();
			if (count < 2)
				return rhs;
			return new StringNode(Strings.repeat(rhs.asString(), (int) count));
		} else if (lhs.isObject() && rhs.isObject()) {
			return mergeRecursive(mapper, (ObjectNode) lhs, (ObjectNode) rhs);
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be multiplied", lhs, rhs);
		}
	}

	private static ObjectNode mergeRecursive(final ObjectMapper mapper, final ObjectNode lhs, final ObjectNode rhs) {
		final ObjectNode result = mapper.createObjectNode();

		for (final Entry<String, JsonNode> e : lhs.properties()) {
			result.set(e.getKey(), e.getValue());
		}

		for (final Entry<String, JsonNode> e : rhs.properties()) {
			final JsonNode l = result.get(e.getKey());
			final JsonNode r = e.getValue();

			JsonNode resolved = r;
			if (l != null && l.isObject() && r.isObject())
				resolved = mergeRecursive(mapper, (ObjectNode) l, (ObjectNode) r);
			result.set(e.getKey(), resolved);
		}
		return result;
	}

	@Override
	public String image() {
		return "*";
	}
}
