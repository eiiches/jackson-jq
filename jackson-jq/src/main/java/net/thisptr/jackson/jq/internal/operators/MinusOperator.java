package net.thisptr.jackson.jq.internal.operators;

import java.util.TreeSet;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class MinusOperator implements BinaryOperator {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		if (lhs.isIntegralNumber() && rhs.isIntegralNumber()) {
			final long r = lhs.asLong() - rhs.asLong();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isNumber() && rhs.isNumber()) {
			final double r = lhs.asDouble() - rhs.asDouble();
			return JsonNodeUtils.asNumericNode(r);
		} else if (lhs.isArray() && rhs.isArray()) {
			final ArrayNode result = mapper.createArrayNode();
			final TreeSet<JsonNode> rset = new TreeSet<JsonNode>(comparator);
			for (final JsonNode r : rhs)
				rset.add(r);
			for (final JsonNode l : lhs)
				if (!rset.contains(l))
					result.add(l);
			return result;
		} else {
			throw new JsonQueryTypeException(lhs, rhs, "cannot be subtracted");
		}
	}

	@Override
	public String image() {
		return "-";
	}
}
