package net.thisptr.jackson.jq.internal.operators;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
		} else if (lhs.isTextual() && rhs.isNumber()) {
			final double count = rhs.asDouble();
			if (count <= 0)
				return NullNode.getInstance();
			if (count < 2)
				return lhs;
			return new TextNode(Strings.repeat(lhs.asText(), (int) count));
		} else if (lhs.isNumber() && rhs.isTextual()) {
			final double count = lhs.asDouble();
			if (count <= 0)
				return NullNode.getInstance();
			if (count < 2)
				return rhs;
			return new TextNode(Strings.repeat(rhs.asText(), (int) count));
		} else if (lhs.isObject() && rhs.isObject()) {
			return mergeRecursive(mapper, (ObjectNode) lhs, (ObjectNode) rhs);
		} else {
			throw new JsonQueryTypeException(lhs, rhs, "cannot be multiplied");
		}
	}

	private static ObjectNode mergeRecursive(final ObjectMapper mapper, final ObjectNode lhs, final ObjectNode rhs) {
		final ObjectNode result = mapper.createObjectNode();

		final Iterator<Entry<String, JsonNode>> liter = lhs.fields();
		while (liter.hasNext()) {
			final Entry<String, JsonNode> e = liter.next();
			result.set(e.getKey(), e.getValue());
		}

		final Iterator<Entry<String, JsonNode>> riter = rhs.fields();
		while (riter.hasNext()) {
			final Entry<String, JsonNode> e = riter.next();
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
