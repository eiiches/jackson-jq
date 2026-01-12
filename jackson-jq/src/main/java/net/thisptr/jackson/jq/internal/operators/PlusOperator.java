package net.thisptr.jackson.jq.internal.operators;

import java.util.Iterator;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class PlusOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(final JsonProvider<JsonNode> jsonProvider, final JsonNode lhs, final JsonNode rhs) throws JsonQueryException {
		final JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		final JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			// This is a bit tricky because Jackson distinguishes between integral and floating point numbers
			// but our JsonNodeType.NUMBER doesn't. 
			// Let's check if they can be represented as longs.
			final double ld = jsonProvider.asDouble(lhs);
			final double rd = jsonProvider.asDouble(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld + (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld + rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			final JsonNode result = jsonProvider.createArray();
			final Iterator<JsonNode> liter = jsonProvider.elements(lhs);
			while (liter.hasNext())
				jsonProvider.add(result, liter.next());
			final Iterator<JsonNode> riter = jsonProvider.elements(rhs);
			while (riter.hasNext())
				jsonProvider.add(result, riter.next());
			return result;
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			return jsonProvider.createString(jsonProvider.asText(lhs) + jsonProvider.asText(rhs));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			final JsonNode result = jsonProvider.createObject();
			final Iterator<Entry<String, JsonNode>> liter = jsonProvider.fields(lhs);
			while (liter.hasNext()) {
				final Entry<String, JsonNode> e = liter.next();
				jsonProvider.set(result, e.getKey(), e.getValue());
			}
			final Iterator<Entry<String, JsonNode>> riter = jsonProvider.fields(rhs);
			while (riter.hasNext()) {
				final Entry<String, JsonNode> e = riter.next();
				jsonProvider.set(result, e.getKey(), e.getValue());
			}
			return result;
		} else if (ltype == JsonNodeType.NULL) {
			return rhs;
		} else if (rtype == JsonNodeType.NULL) {
			return lhs;
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be added", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "+";
	}
}
