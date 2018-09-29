package net.thisptr.jackson.jq.internal.misc;

public class Range {
	public final Long start;
	public final Long end;

	// [start, end)
	public Range(final Long start, final Long end) {
		this.start = start;
		this.end = end;
	}

	public Range over(final long size) {
		long start = this.start != null
				? (this.start < 0 ? this.start + size : this.start)
				: 0;
		long end = this.end != null
				? (this.end < 0 ? this.end + size : this.end)
				: size;
		if (start >= size)
			return new Range(size, size);
		if (start > end || end <= 0)
			return new Range(0L, 0L);
		if (start < 0)
			start = 0;
		if (end > size)
			end = size;
		return new Range(start, end);
	}
}