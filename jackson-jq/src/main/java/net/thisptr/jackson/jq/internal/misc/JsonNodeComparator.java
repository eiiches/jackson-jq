package net.thisptr.jackson.jq.internal.misc;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@SuppressWarnings("serial")
public class JsonNodeComparator implements Comparator<JsonNode>, Serializable {
	private static final JsonNodeComparator defaultInstance = new JsonNodeComparator();

	public static JsonNodeComparator getInstance() {
		return defaultInstance;
	}

	private static JsonNodeType[][] ordering = new JsonNodeType[][] {
			new JsonNodeType[] { JsonNodeType.NULL, JsonNodeType.MISSING },
			new JsonNodeType[] { JsonNodeType.BOOLEAN },
			new JsonNodeType[] { JsonNodeType.NUMBER },
			new JsonNodeType[] { JsonNodeType.STRING, JsonNodeType.BINARY },
			new JsonNodeType[] { JsonNodeType.ARRAY },
			new JsonNodeType[] { JsonNodeType.OBJECT },
	};

	private static Map<JsonNodeType, Integer> orderValues = new HashMap<>();
	static {
		for (int i = 0; i < ordering.length; i++)
			for (final JsonNodeType type : ordering[i])
				orderValues.put(type, i);
	}

	private static int orderValue(final JsonNode node) {
		if (node == null)
			return 0;
		return orderValue(node.getNodeType());
	}

	private static int orderValue(final JsonNodeType type) {
		final Integer value = orderValues.get(type);
		if (value == null)
			throw new IllegalArgumentException("Unknown JsonNodeType: " + type);
		return value;
	}

	// null
	// false
	// true
	// number
	// string, in alphabetical order
	// array, in lexical order
	// object, first compared as arrays in sorted order, then their values
	public int compare(final JsonNode o1, final JsonNode o2) {
		final int r = orderValue(o1) - orderValue(o2);
		if (r != 0)
			return r;

		final JsonNodeType type = o1 != null ? o1.getNodeType() : null;
		if (type == null || type == JsonNodeType.MISSING || type == JsonNodeType.NULL)
			return 0;

		if (type == JsonNodeType.BOOLEAN)
			return Boolean.compare(o1.asBoolean(), o2.asBoolean());

		if (type == JsonNodeType.NUMBER)
			return Double.compare(o1.asDouble(), o2.asDouble());

		if (type == JsonNodeType.STRING || type == JsonNodeType.BINARY)
			return o1.asText().compareTo(o2.asText());

		if (type == JsonNodeType.ARRAY) {
			final int s1 = o1.size();
			final int s2 = o2.size();
			final int s = Math.min(s1, s2);
			for (int i = 0; i < s; ++i) {
				final int rr = compare(o1.get(i), o2.get(i));
				if (rr != 0)
					return rr;
			}
			return Integer.compare(s1, s2);
		}

		if (type == JsonNodeType.OBJECT) {
			final List<String> names1 = Lists.newArrayList(o1.fieldNames());
			final List<String> names2 = Lists.newArrayList(o2.fieldNames());

			// compare by keys
			Collections.sort(names1);
			Collections.sort(names2);
			final int s = Math.min(names1.size(), names2.size());
			for (int i = 0; i < s; ++i) {
				final int rr = names1.get(i).compareTo(names2.get(i));
				if (rr != 0)
					return rr;
			}
			final int rr = Integer.compare(names1.size(), names2.size());
			if (rr != 0)
				return rr;

			// compare by values (keys are sorted alphabetically)
			for (final String name : names1) {
				final int rrr = compare(o1.get(name), o2.get(name));
				if (rrr != 0)
					return rrr;
			}

			return 0;
		}

		throw new IllegalArgumentException("Unknown JsonNodeType: " + type);
	}
}