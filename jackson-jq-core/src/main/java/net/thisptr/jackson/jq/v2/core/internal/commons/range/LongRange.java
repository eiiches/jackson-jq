package net.thisptr.jackson.jq.v2.core.internal.commons.range;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class LongRange {
	public final long startInclusive;
	public final long endExclusive;

	public LongRange(long startInclusive, long endExclusive) {
		if (startInclusive > endExclusive)
			throw new IllegalArgumentException(String.format("startInclusive (%d) must not be greater than endExclusive (%d)", startInclusive, endExclusive));
		this.startInclusive = startInclusive;
		this.endExclusive = endExclusive;
	}

	public static LongRange of(long startInclusive, long endExclusive) {
		return new LongRange(startInclusive, endExclusive);
	}

	public long length() {
		return endExclusive - startInclusive;
	}

	public boolean isEmpty() {
		return startInclusive == endExclusive;
	}

	public boolean contains(long value) {
		return value >= startInclusive && value < endExclusive;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof LongRange))
			return false;
		LongRange other = (LongRange) o;
		return startInclusive == other.startInclusive && endExclusive == other.endExclusive;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startInclusive, endExclusive);
	}

	@Override
	public String toString() {
		return String.format("[%d, %d)", startInclusive, endExclusive);
	}
}
