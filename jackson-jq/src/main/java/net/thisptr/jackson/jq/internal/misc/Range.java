package net.thisptr.jackson.jq.internal.misc;

import com.fasterxml.jackson.databind.JsonNode;

public class Range {
	public final long start;
	public final long end;

	public Range(final long start, final long end) {
		this.start = start;
		this.end = end;
	}

	private static double resolveToPositiveIndex(final JsonNode value, final long size) {
		final double index = value.asDouble();
		if (index < 0)
			return index + size;
		return index;
	}

	public static Range resolve(final JsonNode startNode, final JsonNode endNode, final long size) {
		assert startNode.isNull() || startNode.isNumber();
		assert endNode.isNull() || endNode.isNumber();
		double start = startNode.isNumber()
				? resolveToPositiveIndex(startNode, size)
				: 0;
		double end = endNode.isNumber()
				? resolveToPositiveIndex(endNode, size)
				: size;
		if (start >= size)
			return new Range(size, size);
		if (start > end || end <= 0)
			return new Range(0L, 0L);
		if (start < 0)
			start = 0;
		if (end > size)
			end = size;
		return new Range((long) start, (long) Math.ceil(end));
	}
}