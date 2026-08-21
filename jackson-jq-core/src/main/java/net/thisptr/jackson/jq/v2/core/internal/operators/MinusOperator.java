package net.thisptr.jackson.jq.v2.core.internal.operators;

import java.util.Iterator;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class MinusOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private final @Nullable Version version;

	public MinusOperator() {
		this(null);
	}

	public MinusOperator(@Nullable Version version) {
		this.version = version;
	}

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double ld = jsonProvider.asDouble(lhs);
			double rd = jsonProvider.asDouble(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld - (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld - rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			JsonNode result = jsonProvider.createArray();
			TreeSet<JsonNode> rset = new TreeSet<>(new JsonNodeComparator<>(jsonProvider));
			Iterator<JsonNode> riter = jsonProvider.elements(rhs);
			while (riter.hasNext())
				rset.add(riter.next());
			Iterator<JsonNode> liter = jsonProvider.elements(lhs);
			while (liter.hasNext()) {
				JsonNode l = liter.next();
				if (!rset.contains(l))
					jsonProvider.add(result, l);
			}
			return result;
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be subtracted", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "-";
	}
}
