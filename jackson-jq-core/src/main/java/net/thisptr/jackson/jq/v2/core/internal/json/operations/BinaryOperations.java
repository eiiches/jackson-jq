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
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
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
				throw new JsonQueryException(String.format("%s and %s cannot be divided because the divisor is zero", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs)));
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend / divisor);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			List<JsonNode> result = new ArrayList<>();
			for (String token : Strings.split(jsonProvider.getString(lhs), jsonProvider.getString(rhs)))
				result.add(jsonProvider.createString(token));
			return jsonProvider.createArray(result);
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be divided", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs));
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
			throw new JsonQueryTypeException("%s and %s cannot be subtracted", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs));
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
				throw new JsonQueryException(String.format("%s and %s cannot be divided (remainder) because the divisor is zero", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs)));
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend % divisor);
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be divided (remainder)", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs));
		}
	}

	public static <JsonNode> JsonNode multiply(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
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
			return repeat(jsonProvider, limits, lhs, jsonProvider.getNumberAsDoubleRounded(rhs));
		} else if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.STRING) {
			return repeat(jsonProvider, limits, rhs, jsonProvider.getNumberAsDoubleRounded(lhs));
		} else if (ltype == JsonNodeType.OBJECT && rtype == JsonNodeType.OBJECT) {
			return mergeRecursive(jsonProvider, limits, lhs, rhs);
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be multiplied", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs));
		}
	}

	private static <JsonNode> JsonNode repeat(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, JsonNode str, double count) {
		if (count <= 0)
			return jsonProvider.createNull();
		if (count < 2)
			return str;
		String text = jsonProvider.getString(str);
		// A count beyond int range saturates to Integer.MAX_VALUE, which the check below rejects for
		// any non-empty string long before Strings.repeat could overflow its capacity.
		int n = (int) count;
		RuntimeLimitChecks.checkStringLength(limits, (long) text.length() * n);
		return jsonProvider.createString(Strings.repeat(text, n));
	}

	private static <JsonNode> JsonNode mergeRecursive(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, JsonNode lhs, JsonNode rhs) {
		Map<String, JsonNode> result = new LinkedHashMap<>();

		Iterator<Map.Entry<String, JsonNode>> liter = jsonProvider.getObjectMembers(lhs);
		while (liter.hasNext()) {
			Map.Entry<String, JsonNode> e = liter.next();
			result.put(e.getKey(), e.getValue());
		}

		Iterator<Map.Entry<String, JsonNode>> riter = jsonProvider.getObjectMembers(rhs);
		while (riter.hasNext()) {
			Map.Entry<String, JsonNode> e = riter.next();
			@Var JsonNode l = result.get(e.getKey());
			if (l == null) // no such member
				l = jsonProvider.createNull();
			JsonNode r = e.getValue();

			@Var JsonNode resolved = r;
			if (jsonProvider.isObject(l) && jsonProvider.isObject(r))
				resolved = mergeRecursive(jsonProvider, limits, l, r);
			result.put(e.getKey(), resolved);
		}
		RuntimeLimitChecks.checkObjectSize(limits, result.size());
		return jsonProvider.createObject(result);
	}

	public static <JsonNode> JsonNode plus(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, JsonNode lhs, JsonNode rhs, Version version) throws JsonQueryException {
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
			RuntimeLimitChecks.checkArraySize(limits, (long) jsonProvider.getArrayLength(lhs) + jsonProvider.getArrayLength(rhs));
			List<JsonNode> values = new ArrayList<>(jsonProvider.getArrayLength(lhs) + jsonProvider.getArrayLength(rhs));
			Iterator<JsonNode> liter = jsonProvider.getArrayElements(lhs);
			while (liter.hasNext())
				values.add(liter.next());
			Iterator<JsonNode> riter = jsonProvider.getArrayElements(rhs);
			while (riter.hasNext())
				values.add(riter.next());
			return jsonProvider.createArray(values);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			String l = jsonProvider.getString(lhs);
			String r = jsonProvider.getString(rhs);
			RuntimeLimitChecks.checkStringLength(limits, (long) l.length() + r.length());
			return jsonProvider.createString(l + r);
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
			RuntimeLimitChecks.checkObjectSize(limits, values.size());
			return jsonProvider.createObject(values);
		} else if (ltype == JsonNodeType.NULL) {
			return rhs;
		} else if (rtype == JsonNodeType.NULL) {
			return lhs;
		} else {
			throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, lhs), ExceptionMessages.describe(jsonProvider, version, rhs));
		}
	}

	public static <JsonNode> JsonNode alternative(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) {
		return JsonNodeUtils.asBoolean(jsonProvider, lhs) ? lhs : rhs;
	}
}
