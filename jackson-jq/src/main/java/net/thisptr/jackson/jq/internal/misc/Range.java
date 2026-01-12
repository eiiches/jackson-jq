package net.thisptr.jackson.jq.internal.misc;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class Range {
	public final long start;
	public final long end;

	public Range(final long start, final long end) {
		this.start = start;
		this.end = end;
	}

	private static <JsonNode> double resolveToPositiveIndex(final JsonProvider<JsonNode> jsonProvider, final JsonNode value, final long size) {
		final double index = jsonProvider.asDouble(value);
		if (index < 0)
			return index + size;
		return index;
	}

	public static <JsonNode> Range resolve(final JsonProvider<JsonNode> jsonProvider, final JsonNode startNode, final JsonNode endNode, final long size) {
		assert jsonProvider.getNodeType(startNode) == JsonNodeType.NULL || jsonProvider.getNodeType(startNode) == JsonNodeType.NUMBER;
		assert jsonProvider.getNodeType(endNode) == JsonNodeType.NULL || jsonProvider.getNodeType(endNode) == JsonNodeType.NUMBER;
		double start = jsonProvider.getNodeType(startNode) == JsonNodeType.NUMBER
				? resolveToPositiveIndex(jsonProvider, startNode, size)
				: 0;
		double end = jsonProvider.getNodeType(endNode) == JsonNodeType.NUMBER
				? resolveToPositiveIndex(jsonProvider, endNode, size)
				: size;
		if (start >= size)
			return new Range(size, size);
		if (start < 0)
			start = 0;
		if (end > size)
			end = size;
		if (start > end)
			return new Range((long) start, (long) start);
		return new Range((long) start, (long) Math.ceil(end));
	}
}
