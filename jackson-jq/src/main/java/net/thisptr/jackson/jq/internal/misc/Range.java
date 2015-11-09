package net.thisptr.jackson.jq.internal.misc;

public class Range {
	public final long end;
	public final long begin;

	// [begin, end)
	public Range(final long begin, final long end) {
		this.begin = begin;
		this.end = end;
	}

	public Range over(final int size) {
		long begin = this.begin < 0 ? this.begin + size : this.begin;
		long end = this.end < 0 ? this.end + size : this.end;
		if (begin >= size)
			return new Range(size, size);
		if (begin > end || end <= 0)
			return new Range(0, 0);
		if (begin < 0)
			begin = 0;
		if (end > size)
			end = size;
		return new Range(begin, end);
	}
}