package net.thisptr.jackson.jq.internal.operators;

import java.util.Iterator;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class MultiplyOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		final JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			final double ld = jsonProvider.asDouble(lhs);
			final double rd = jsonProvider.asDouble(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld * (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld * rd);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.NUMBER) {
			final double count = jsonProvider.asDouble(rhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return lhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.asText(lhs), (int) count));
		} else if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.STRING) {
			final double count = jsonProvider.asDouble(lhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return rhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.asText(rhs), (int) count));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			return mergeRecursive(jsonProvider, lhs, rhs);
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be multiplied", lhs, rhs);
		}
	}

	private static <JsonNode> JsonNode mergeRecursive(final JsonProvider<JsonNode> jsonProvider, final JsonNode lhs, final JsonNode rhs) {
		final JsonNode result = jsonProvider.createObject();

		final Iterator<Entry<String, JsonNode>> liter = jsonProvider.fields(lhs);
		while (liter.hasNext()) {
			final Entry<String, JsonNode> e = liter.next();
			jsonProvider.set(result, e.getKey(), e.getValue());
		}

		final Iterator<Entry<String, JsonNode>> riter = jsonProvider.fields(rhs);
		while (riter.hasNext()) {
			final Entry<String, JsonNode> e = riter.next();
			final JsonNode l = jsonProvider.get(result, e.getKey());
			final JsonNode r = e.getValue();

			JsonNode resolved = r;
			if (l != null && jsonProvider.getNodeType(l) == JsonNodeType.OBJECT && jsonProvider.getNodeType(r) == JsonNodeType.OBJECT)
				resolved = mergeRecursive(jsonProvider, l, r);
			jsonProvider.set(result, e.getKey(), resolved);
		}
		return result;
	}

	@Override
	public String image() {
		return "*";
	}
}
