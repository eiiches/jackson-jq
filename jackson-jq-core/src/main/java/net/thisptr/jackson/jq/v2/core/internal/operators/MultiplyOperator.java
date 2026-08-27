package net.thisptr.jackson.jq.v2.core.internal.operators;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class MultiplyOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private final @Nullable Version version;

	public MultiplyOperator() {
		this(null);
	}

	public MultiplyOperator(@Nullable Version version) {
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
				return JsonNodeUtils.asNumericNode(jsonProvider, ((long) ld) * (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld * rd);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.NUMBER) {
			double count = jsonProvider.asDouble(rhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return lhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.asText(lhs), (int) count));
		} else if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.STRING) {
			double count = jsonProvider.asDouble(lhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return rhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.asText(rhs), (int) count));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			return mergeRecursive(jsonProvider, lhs, rhs);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be multiplied", lhs, rhs);
		}
	}

	private static <JsonNode> JsonNode mergeRecursive(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) {
		JsonNode result = jsonProvider.createObject();

		Iterator<Map.Entry<String, JsonNode>> liter = jsonProvider.fields(lhs);
		while (liter.hasNext()) {
			Map.Entry<String, JsonNode> e = liter.next();
			jsonProvider.set(result, e.getKey(), e.getValue());
		}

		Iterator<Map.Entry<String, JsonNode>> riter = jsonProvider.fields(rhs);
		while (riter.hasNext()) {
			Map.Entry<String, JsonNode> e = riter.next();
			JsonNode l = jsonProvider.get(result, e.getKey());
			JsonNode r = e.getValue();

			@Var JsonNode resolved = r;
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
