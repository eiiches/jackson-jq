package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

@NullMarked
public class JsonNodeComparator<JsonNode> implements Comparator<JsonNode>, Serializable {
	protected final JsonProvider<JsonNode> jsonProvider;

	public JsonNodeComparator(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	private static final JsonNodeType[] TYPE_ORDER = new JsonNodeType[] {
			JsonNodeType.NULL,
			JsonNodeType.BOOLEAN,
			JsonNodeType.NUMBER,
			JsonNodeType.STRING,
			JsonNodeType.BINARY,
			JsonNodeType.ARRAY,
			JsonNodeType.OBJECT,
	};

	private static final Map<JsonNodeType, Integer> TYPE_ORDER_MAP = new HashMap<>();

	static {
		for (int i = 0; i < TYPE_ORDER.length; i++)
			TYPE_ORDER_MAP.put(TYPE_ORDER[i], i);
	}

	private int orderValue(JsonNode node) {
		return orderValue(jsonProvider.getNodeType(node));
	}

	private static int orderValue(JsonNodeType type) {
		Integer value = TYPE_ORDER_MAP.get(type);
		if (value == null)
			throw new IllegalArgumentException("Unknown JsonNodeType: " + type);
		return value;
	}

	protected int compareNumberNode(JsonNode o1, JsonNode o2) {
		double a = jsonProvider.getNumberAsDoubleRounded(o1);
		double b = jsonProvider.getNumberAsDoubleRounded(o2);
		if (Double.isNaN(a))
			return -1;
		if (Double.isNaN(b))
			return 1;
		// Rounding to double is monotonic, so whenever the doubles differ their order is already
		// the exact order; only a tie can hide a difference in the exact values (e.g.
		// 2871948651097801136 vs 2871948651097801137, both 2.871948651097801E18 as double).
		if (a < b)
			return -1;
		if (a > b)
			return 1;
		BigDecimal x = jsonProvider.getNumberAsBigDecimalExact(o1);
		BigDecimal y = jsonProvider.getNumberAsBigDecimalExact(o2);
		if (x == null || y == null)
			return 0; // Infinity/-Infinity on at least one side; already ordered by double
		return x.compareTo(y);
	}

	protected int compareArrayNode(JsonNode o1, JsonNode o2) {
		int s1 = jsonProvider.getArrayLength(o1);
		int s2 = jsonProvider.getArrayLength(o2);
		int s = Math.min(s1, s2);
		for (int i = 0; i < s; ++i) {
			int rr = compare(jsonProvider.getArrayElement(o1, i), jsonProvider.getArrayElement(o2, i));
			if (rr != 0)
				return rr;
		}
		return Integer.compare(s1, s2);
	}

	protected int compareObjectNode(JsonNode o1, JsonNode o2) {
		List<String> names1 = Lists.newArrayList(jsonProvider.getObjectFieldNames(o1));
		List<String> names2 = Lists.newArrayList(jsonProvider.getObjectFieldNames(o2));

		// compare by keys
		Collections.sort(names1);
		Collections.sort(names2);
		int s = Math.min(names1.size(), names2.size());
		for (int i = 0; i < s; ++i) {
			int rr = names1.get(i).compareTo(names2.get(i));
			if (rr != 0)
				return rr;
		}
		int rr = Integer.compare(names1.size(), names2.size());
		if (rr != 0)
			return rr;

		// compare by values (keys are sorted alphabetically)
		for (String name : names1) {
			int rrr = compare(jsonProvider.getObjectFieldOrThrow(o1, name), jsonProvider.getObjectFieldOrThrow(o2, name));
			if (rrr != 0)
				return rrr;
		}

		return 0;
	}

	/**
	 * Binary is not a JSON type, so such a node can only arrive as caller-supplied input. It gets its
	 * own order class, between strings and arrays, and is compared byte by byte.
	 */
	protected int compareBinaryNode(JsonNode o1, JsonNode o2) {
		byte[] b1 = jsonProvider.getBinaryAsByteArray(o1);
		byte[] b2 = jsonProvider.getBinaryAsByteArray(o2);
		int s = Math.min(b1.length, b2.length);
		for (int i = 0; i < s; ++i) {
			// Unsigned, so that 0xff sorts after 0x01 rather than before it.
			int rr = Integer.compare(b1[i] & 0xff, b2[i] & 0xff);
			if (rr != 0)
				return rr;
		}
		return Integer.compare(b1.length, b2.length);
	}

	// null
	// false
	// true
	// number
	// string, in alphabetical order
	// binary, in lexical byte order
	// array, in lexical order
	// object, first compared as arrays in sorted order, then their values
	@Override
	public int compare(JsonNode o1, JsonNode o2) {
		JsonNodeType type1 = jsonProvider.getNodeType(o1);
		JsonNodeType type2 = jsonProvider.getNodeType(o2);

		if (type1 != type2)
			return Integer.compare(orderValue(o1), orderValue(o2));

		switch (type1) {
			case NULL:
				return 0;
			case BOOLEAN:
				return Boolean.compare(jsonProvider.getBoolean(o1), jsonProvider.getBoolean(o2));
			case NUMBER:
				return compareNumberNode(o1, o2);
			case STRING:
				return jsonProvider.getString(o1).compareTo(jsonProvider.getString(o2));
			case BINARY:
				return compareBinaryNode(o1, o2);
			case ARRAY:
				return compareArrayNode(o1, o2);
			case OBJECT:
				return compareObjectNode(o1, o2);
			default:
				throw new IllegalArgumentException("Unknown JsonNodeType: " + type1);
		}
	}
}
