package net.thisptr.jackson.jq.internal.operators;

import java.util.Iterator;
import java.util.TreeSet;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class MinusOperator<JsonNode> implements BinaryOperator<JsonNode> {
	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		final JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			final double ld = jsonProvider.asDouble(lhs);
			final double rd = jsonProvider.asDouble(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld - (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld - rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			final JsonNode result = jsonProvider.createArray();
			final TreeSet<JsonNode> rset = new TreeSet<>(new JsonNodeComparator<>(jsonProvider));
			final Iterator<JsonNode> riter = jsonProvider.elements(rhs);
			while (riter.hasNext())
				rset.add(riter.next());
			final Iterator<JsonNode> liter = jsonProvider.elements(lhs);
			while (liter.hasNext()) {
				final JsonNode l = liter.next();
				if (!rset.contains(l))
					jsonProvider.add(result, l);
			}
			return result;
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be subtracted", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "-";
	}
}
