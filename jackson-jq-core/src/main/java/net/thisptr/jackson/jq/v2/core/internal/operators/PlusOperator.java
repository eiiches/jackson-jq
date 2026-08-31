package net.thisptr.jackson.jq.v2.core.internal.operators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class PlusOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private final @Nullable Version version;

	public PlusOperator() {
		this(null);
	}

	public PlusOperator(@Nullable Version version) {
		this.version = version;
	}

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			// This is a bit tricky because Jackson distinguishes between integral and floating point numbers
			// but our JsonNodeType.NUMBER doesn't.
			// Let's check if they can be represented as longs.
			double ld = jsonProvider.asDoubleRounded(lhs);
			double rd = jsonProvider.asDoubleRounded(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld + (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld + rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			List<JsonNode> values = new ArrayList<>(jsonProvider.size(lhs) + jsonProvider.size(rhs));
			Iterator<JsonNode> liter = jsonProvider.elements(lhs);
			while (liter.hasNext())
				values.add(liter.next());
			Iterator<JsonNode> riter = jsonProvider.elements(rhs);
			while (riter.hasNext())
				values.add(riter.next());
			return jsonProvider.createArray(values);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			return jsonProvider.createString(jsonProvider.asString(lhs) + jsonProvider.asString(rhs));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			Map<String, JsonNode> values = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> liter = jsonProvider.fields(lhs);
			while (liter.hasNext()) {
				Map.Entry<String, JsonNode> e = liter.next();
				values.put(e.getKey(), e.getValue());
			}
			Iterator<Map.Entry<String, JsonNode>> riter = jsonProvider.fields(rhs);
			while (riter.hasNext()) {
				Map.Entry<String, JsonNode> e = riter.next();
				values.put(e.getKey(), e.getValue());
			}
			return jsonProvider.createObject(values);
		} else if (ltype == JsonNodeType.NULL) {
			return rhs;
		} else if (rtype == JsonNodeType.NULL) {
			return lhs;
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be added", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "+";
	}
}
