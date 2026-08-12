package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

@SuppressWarnings("serial")
public class JsonNodeComparator<JsonNode> implements Comparator<JsonNode>, Serializable {
	private final JsonProvider<JsonNode> jsonProvider;

	public JsonNodeComparator(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	private static final JsonNodeType[][] ordering = new JsonNodeType[][] {
			new JsonNodeType[] { JsonNodeType.NULL, JsonNodeType.MISSING },
			new JsonNodeType[] { JsonNodeType.BOOLEAN },
			new JsonNodeType[] { JsonNodeType.NUMBER },
			new JsonNodeType[] { JsonNodeType.STRING, JsonNodeType.BINARY },
			new JsonNodeType[] { JsonNodeType.ARRAY },
			new JsonNodeType[] { JsonNodeType.OBJECT },
	};

	private static final Map<JsonNodeType, Integer> orderValues = new HashMap<>();
	static {
		for (int i = 0; i < ordering.length; i++)
			for (JsonNodeType type : ordering[i])
				orderValues.put(type, i);
	}

	private int orderValue(JsonNode node) {
		if (node == null)
			return 0;
		return orderValue(jsonProvider.getNodeType(node));
	}

	private static int orderValue(JsonNodeType type) {
		Integer value = orderValues.get(type);
		if (value == null)
			throw new IllegalArgumentException("Unknown JsonNodeType: " + type);
		return value;
	}

	protected int compareNumberNode(JsonNode o1, JsonNode o2) {
		double a = jsonProvider.asDouble(o1);
		double b = jsonProvider.asDouble(o2);
		if (Double.isNaN(a))
			return -1;
		if (Double.isNaN(b))
			return 1;
		return Double.compare(a, b);
	}

	protected int compareArrayNode(JsonNode o1, JsonNode o2) {
		int s1 = jsonProvider.size(o1);
		int s2 = jsonProvider.size(o2);
		int s = Math.min(s1, s2);
		for (int i = 0; i < s; ++i) {
			int rr = compare(jsonProvider.get(o1, i), jsonProvider.get(o2, i));
			if (rr != 0)
				return rr;
		}
		return Integer.compare(s1, s2);
	}

	protected int compareObjectNode(JsonNode o1, JsonNode o2) {
		List<String> names1 = Lists.newArrayList(jsonProvider.fieldNames(o1));
		List<String> names2 = Lists.newArrayList(jsonProvider.fieldNames(o2));

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
			int rrr = compare(jsonProvider.get(o1, name), jsonProvider.get(o2, name));
			if (rrr != 0)
				return rrr;
		}

		return 0;
	}

	// null
	// false
	// true
	// number
	// string, in alphabetical order
	// array, in lexical order
	// object, first compared as arrays in sorted order, then their values
	@Override
	public int compare(JsonNode o1, JsonNode o2) {
		int r = orderValue(o1) - orderValue(o2);
		if (r != 0)
			return r;

		JsonNodeType type = o1 != null ? jsonProvider.getNodeType(o1) : null;
		if (type == null || type == JsonNodeType.MISSING || type == JsonNodeType.NULL)
			return 0;

		if (type == JsonNodeType.BOOLEAN)
			return Boolean.compare(jsonProvider.asBoolean(o1), jsonProvider.asBoolean(o2));

		if (type == JsonNodeType.NUMBER) {
			return compareNumberNode(o1, o2);
		}

		if (type == JsonNodeType.STRING || type == JsonNodeType.BINARY)
			return jsonProvider.asText(o1).compareTo(jsonProvider.asText(o2));

		if (type == JsonNodeType.ARRAY) {
			return compareArrayNode(o1, o2);
		}

		if (type == JsonNodeType.OBJECT) {
			return compareObjectNode(o1, o2);
		}

		throw new IllegalArgumentException("Unknown JsonNodeType: " + type);
	}
}
