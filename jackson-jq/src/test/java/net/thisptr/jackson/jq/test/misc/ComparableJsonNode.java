package net.thisptr.jackson.jq.test.misc;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class ComparableJsonNode implements Comparable<ComparableJsonNode> {
	private final JsonNode value;

	public ComparableJsonNode(final JsonNode value) {
		this.value = value;
	}

	@Override
	public int compareTo(final ComparableJsonNode o) {
		return JsonNodeComparator.getInstance().compare(this.value, o.value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ComparableJsonNode))
			return false;
		return compareTo((ComparableJsonNode) obj) == 0;
	}

	public static List<ComparableJsonNode> wrap(final List<JsonNode> values) {
		return values.stream().map(ComparableJsonNode::new).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}