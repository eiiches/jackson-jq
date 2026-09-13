package net.thisptr.jackson.jq.v2.core.internal.misc;

import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;

/**
 * The engine's own immutable {@link RuntimeLimits}. Callers configure it through
 * {@code RuntimeOptions}; {@link #UNLIMITED} is what every evaluation runs under until they do.
 */
public final class RuntimeLimitsImpl implements RuntimeLimits {
	/**
	 * Bounds nothing -- the behaviour of every release before limits existed.
	 */
	public static final RuntimeLimitsImpl UNLIMITED = new RuntimeLimitsImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final int maxArrayLength;
	private final int maxObjectMemberCount;
	private final int maxStringLength;

	public RuntimeLimitsImpl(int maxArrayLength, int maxObjectMemberCount, int maxStringLength) {
		if (maxArrayLength < 0)
			throw new IllegalArgumentException("maxArrayLength must not be negative");
		if (maxObjectMemberCount < 0)
			throw new IllegalArgumentException("maxObjectMemberCount must not be negative");
		if (maxStringLength < 0)
			throw new IllegalArgumentException("maxStringLength must not be negative");
		this.maxArrayLength = maxArrayLength;
		this.maxObjectMemberCount = maxObjectMemberCount;
		this.maxStringLength = maxStringLength;
	}

	@Override
	public int getMaxArrayLength() {
		return maxArrayLength;
	}

	@Override
	public int getMaxObjectMemberCount() {
		return maxObjectMemberCount;
	}

	@Override
	public int getMaxStringLength() {
		return maxStringLength;
	}

	@Override
	public String toString() {
		return "RuntimeLimits(maxArrayLength=" + maxArrayLength + ", maxObjectMemberCount=" + maxObjectMemberCount + ", maxStringLength=" + maxStringLength + ")";
	}
}
