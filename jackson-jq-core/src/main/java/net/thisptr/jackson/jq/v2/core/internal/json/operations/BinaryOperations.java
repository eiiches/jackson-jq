package net.thisptr.jackson.jq.v2.core.internal.json.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.Strings;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class BinaryOperations {
	private BinaryOperations() {
	}

	public static <JsonNode> JsonNode divide(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double divisor = jsonProvider.getNumberAsDoubleRounded(rhs);
			double dividend = jsonProvider.getNumberAsDoubleRounded(lhs);
			if (divisor == 0.0)
				throw new JsonQueryException(ExceptionMessages.format(jsonProvider, version, "%s and %s cannot be divided because the divisor is zero", lhs, rhs));
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend / divisor);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			List<JsonNode> result = new ArrayList<>();
			for (String token : Strings.split(jsonProvider.getString(lhs), jsonProvider.getString(rhs)))
				result.add(jsonProvider.createString(token));
			return jsonProvider.createArray(result);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be divided", lhs, rhs);
		}
	}

	public static <JsonNode> JsonNode minus(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
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

	public static <JsonNode> JsonNode modulo(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double lhsDouble = jsonProvider.getNumberAsDoubleRounded(lhs);
			double rhsDouble = jsonProvider.getNumberAsDoubleRounded(rhs);

			// Handle Infinity: convert to long representation
			long dividend = Double.isNaN(lhsDouble) ? 0L
					: Double.isInfinite(lhsDouble) ? (lhsDouble > 0 ? Long.MAX_VALUE : Long.MIN_VALUE)
					: (long) lhsDouble;

			// If divisor is NaN, return the dividend (jq 1.5 behavior)
			if (Double.isNaN(rhsDouble))
				return JsonNodeUtils.asNumericNode(jsonProvider, dividend);

			long divisor = Double.isInfinite(rhsDouble)
					? (rhsDouble > 0 ? Long.MAX_VALUE : Long.MIN_VALUE)
					: (long) rhsDouble;

			if (divisor == 0L)
				throw new JsonQueryException(ExceptionMessages.format(jsonProvider, version, "%s and %s cannot be divided (remainder) because the divisor is zero", lhs, rhs));
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend % divisor);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be divided (remainder)", lhs, rhs);
		}
	}

	public static <JsonNode> JsonNode multiply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double ld = jsonProvider.getNumberAsDoubleRounded(lhs);
			double rd = jsonProvider.getNumberAsDoubleRounded(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, ((long) ld) * (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld * rd);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.NUMBER) {
			double count = jsonProvider.getNumberAsDoubleRounded(rhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return lhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.getString(lhs), (int) count));
		} else if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.STRING) {
			double count = jsonProvider.getNumberAsDoubleRounded(lhs);
			if (count <= 0)
				return jsonProvider.createNull();
			if (count < 2)
				return rhs;
			return jsonProvider.createString(Strings.repeat(jsonProvider.getString(rhs), (int) count));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			return mergeRecursive(jsonProvider, lhs, rhs);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be multiplied", lhs, rhs);
		}
	}

	private static <JsonNode> JsonNode mergeRecursive(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) {
		Map<String, JsonNode> result = new LinkedHashMap<>();

		Iterator<Map.Entry<String, JsonNode>> liter = jsonProvider.getObjectMembers(lhs);
		while (liter.hasNext()) {
			Map.Entry<String, JsonNode> e = liter.next();
			result.put(e.getKey(), e.getValue());
		}

		Iterator<Map.Entry<String, JsonNode>> riter = jsonProvider.getObjectMembers(rhs);
		while (riter.hasNext()) {
			Map.Entry<String, JsonNode> e = riter.next();
			JsonNode l = result.get(e.getKey());
			JsonNode r = e.getValue();

			@Var JsonNode resolved = r;
			if (l != null && jsonProvider.isObject(l) && jsonProvider.isObject(r))
				resolved = mergeRecursive(jsonProvider, l, r);
			result.put(e.getKey(), resolved);
		}
		return jsonProvider.createObject(result);
	}

	public static <JsonNode> JsonNode plus(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double ld = jsonProvider.getNumberAsDoubleRounded(lhs);
			double rd = jsonProvider.getNumberAsDoubleRounded(rhs);
			if (ld == (long) ld && rd == (long) rd) {
				return JsonNodeUtils.asNumericNode(jsonProvider, (long) ld + (long) rd);
			}
			return JsonNodeUtils.asNumericNode(jsonProvider, ld + rd);
		} else if (ltype == JsonNodeType.ARRAY && rtype == JsonNodeType.ARRAY) {
			List<JsonNode> values = new ArrayList<>(jsonProvider.getArrayLength(lhs) + jsonProvider.getArrayLength(rhs));
			Iterator<JsonNode> liter = jsonProvider.getArrayElements(lhs);
			while (liter.hasNext())
				values.add(liter.next());
			Iterator<JsonNode> riter = jsonProvider.getArrayElements(rhs);
			while (riter.hasNext())
				values.add(riter.next());
			return jsonProvider.createArray(values);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			return jsonProvider.createString(jsonProvider.getString(lhs) + jsonProvider.getString(rhs));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			Map<String, JsonNode> values = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> liter = jsonProvider.getObjectMembers(lhs);
			while (liter.hasNext()) {
				Map.Entry<String, JsonNode> e = liter.next();
				values.put(e.getKey(), e.getValue());
			}
			Iterator<Map.Entry<String, JsonNode>> riter = jsonProvider.getObjectMembers(rhs);
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

	public static <JsonNode> JsonNode alternative(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) {
		return JsonNodeUtils.asBoolean(jsonProvider, lhs) ? lhs : rhs;
	}
}
