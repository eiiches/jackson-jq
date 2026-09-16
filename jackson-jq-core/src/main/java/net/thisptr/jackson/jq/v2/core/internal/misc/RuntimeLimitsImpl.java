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
	public static final RuntimeLimitsImpl UNLIMITED = new RuntimeLimitsImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE);

	private final int maxArrayLength;
	private final int maxObjectMemberCount;
	private final int maxStringLength;
	private final long maxUserDefinedFunctionCalls;

	public RuntimeLimitsImpl(int maxArrayLength, int maxObjectMemberCount, int maxStringLength, long maxUserDefinedFunctionCalls) {
		if (maxArrayLength < 0)
			throw new IllegalArgumentException("maxArrayLength must not be negative");
		if (maxObjectMemberCount < 0)
			throw new IllegalArgumentException("maxObjectMemberCount must not be negative");
		if (maxStringLength < 0)
			throw new IllegalArgumentException("maxStringLength must not be negative");
		if (maxUserDefinedFunctionCalls < 0)
			throw new IllegalArgumentException("maxUserDefinedFunctionCalls must not be negative");
		this.maxArrayLength = maxArrayLength;
		this.maxObjectMemberCount = maxObjectMemberCount;
		this.maxStringLength = maxStringLength;
		this.maxUserDefinedFunctionCalls = maxUserDefinedFunctionCalls;
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

	/**
	 * Returns the largest number of query-text {@code def} calls one evaluation may make.
	 * <p>
	 * Engine-only: deliberately absent from {@link RuntimeLimits}, because the engine enforces this at the
	 * {@code def} call site itself and no {@code Expression}/{@code Function} implementation could act on
	 * it. Enforced by {@code Memory#countUserDefinedFunctionCall}, the one limit that counts rather than
	 * sizes.
	 *
	 * @return the maximum number of user-defined function calls, or {@link Long#MAX_VALUE} for no limit
	 */
	public long getMaxUserDefinedFunctionCalls() {
		return maxUserDefinedFunctionCalls;
	}

	@Override
	public String toString() {
		return "RuntimeLimits(maxArrayLength=" + maxArrayLength + ", maxObjectMemberCount=" + maxObjectMemberCount + ", maxStringLength=" + maxStringLength + ", maxUserDefinedFunctionCalls=" + maxUserDefinedFunctionCalls + ")";
	}
}
