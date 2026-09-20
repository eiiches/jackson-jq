package net.thisptr.jackson.jq.v2.core.internal.commons.range;

public record LongRange(long startInclusive, long endExclusive) {
	public LongRange {
		if (startInclusive > endExclusive)
			throw new IllegalArgumentException(String.format("startInclusive (%d) must not be greater than endExclusive (%d)", startInclusive, endExclusive));
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
	public String toString() {
		return String.format("[%d, %d)", startInclusive, endExclusive);
	}
}
