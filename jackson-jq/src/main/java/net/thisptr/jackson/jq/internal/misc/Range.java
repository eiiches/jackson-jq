package net.thisptr.jackson.jq.internal.misc;

import com.fasterxml.jackson.databind.JsonNode;

public class Range {
	public long start;
	public long end;

	public Range(final long start, final long end) {
		this.start = start;
		this.end = end;
	}

	public Range() {}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
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
		if (start < 0)
			start = 0;
		if (end > size)
			end = size;
		if (start > end)
			return new Range((long) start, (long) start);
		return new Range((long) start, (long) Math.ceil(end));
	}
}