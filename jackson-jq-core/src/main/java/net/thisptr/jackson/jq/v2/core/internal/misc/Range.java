package net.thisptr.jackson.jq.v2.core.internal.misc;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class Range {
	public final long start;
	public final long end;

	public Range(long start, long end) {
		this.start = start;
		this.end = end;
	}

	private static <JsonNode> double resolveToPositiveIndex(JsonProvider<JsonNode> jsonProvider, JsonNode value, long size) {
		double index = jsonProvider.asDoubleRounded(value);
		if (index < 0)
			return index + size;
		return index;
	}

	public static <JsonNode> Range resolve(JsonProvider<JsonNode> jsonProvider, JsonNode startNode, JsonNode endNode, long size) {
		assert jsonProvider.getNodeType(startNode) == JsonNodeType.NULL || jsonProvider.getNodeType(startNode) == JsonNodeType.NUMBER;
		assert jsonProvider.getNodeType(endNode) == JsonNodeType.NULL || jsonProvider.getNodeType(endNode) == JsonNodeType.NUMBER;
		@Var double start = jsonProvider.getNodeType(startNode) == JsonNodeType.NUMBER
				? resolveToPositiveIndex(jsonProvider, startNode, size)
				: 0;
		@Var double end = jsonProvider.getNodeType(endNode) == JsonNodeType.NUMBER
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
