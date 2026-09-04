package net.thisptr.jackson.jq.v2.core.internal.operators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

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
			double ld = jsonProvider.getNumberAsDoubleRounded(lhs);
			double rd = jsonProvider.getNumberAsDoubleRounded(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld - (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld - rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			List<JsonNode> result = new ArrayList<>();
			TreeSet<JsonNode> rset = new TreeSet<>(new JsonNodeComparator<>(jsonProvider));
			Iterator<JsonNode> riter = jsonProvider.getArrayElements(rhs);
			while (riter.hasNext())
				rset.add(riter.next());
			Iterator<JsonNode> liter = jsonProvider.getArrayElements(lhs);
			while (liter.hasNext()) {
				JsonNode l = liter.next();
				if (!rset.contains(l))
					result.add(l);
			}
			return jsonProvider.createArray(result);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be subtracted", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "-";
	}
}
