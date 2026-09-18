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
	public static final RuntimeLimitsImpl UNLIMITED = new RuntimeLimitsImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

	private final int maxArrayLength;
	private final int maxObjectMemberCount;
	private final int maxStringLength;
	private final int maxBinaryLength;
	private final long maxUserDefinedFunctionCalls;
	private final long maxOutputsPerExpression;

	public RuntimeLimitsImpl(int maxArrayLength, int maxObjectMemberCount, int maxStringLength, int maxBinaryLength, long maxUserDefinedFunctionCalls, long maxOutputsPerExpression) {
		if (maxArrayLength < 0)
			throw new IllegalArgumentException("maxArrayLength must not be negative");
		if (maxObjectMemberCount < 0)
			throw new IllegalArgumentException("maxObjectMemberCount must not be negative");
		if (maxStringLength < 0)
			throw new IllegalArgumentException("maxStringLength must not be negative");
		if (maxBinaryLength < 0)
			throw new IllegalArgumentException("maxBinaryLength must not be negative");
		if (maxUserDefinedFunctionCalls < 0)
			throw new IllegalArgumentException("maxUserDefinedFunctionCalls must not be negative");
		if (maxOutputsPerExpression < 0)
			throw new IllegalArgumentException("maxOutputsPerExpression must not be negative");
		this.maxArrayLength = maxArrayLength;
		this.maxObjectMemberCount = maxObjectMemberCount;
		this.maxStringLength = maxStringLength;
		this.maxBinaryLength = maxBinaryLength;
		this.maxUserDefinedFunctionCalls = maxUserDefinedFunctionCalls;
		this.maxOutputsPerExpression = maxOutputsPerExpression;
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
	public int getMaxBinaryLength() {
		return maxBinaryLength;
	}

	/**
	 * Returns the largest number of query-text {@code def} calls one evaluation may make.
	 * <p>
	 * Engine-only: deliberately absent from {@link RuntimeLimits}, because the engine enforces this at the
	 * {@code def} call site itself and no {@code Expression}/{@code Function} implementation could act on
	 * it. Enforced by {@code Memory#countUserDefinedFunctionCall}, one of the two limits that count rather
	 * than size.
	 *
	 * @return the maximum number of user-defined function calls, or {@link Long#MAX_VALUE} for no limit
	 */
	public long getMaxUserDefinedFunctionCalls() {
		return maxUserDefinedFunctionCalls;
	}

	/**
	 * Returns the largest number of values one query-text expression may emit during one evaluation.
	 * <p>
	 * Engine-only, for the same reason as {@link #getMaxUserDefinedFunctionCalls()}: the engine tallies
	 * each expression's output at the node boundary the compiler put there, and an
	 * {@code Expression}/{@code Function} implementation could neither see its own tally nor act on it --
	 * whatever it emits is already charged to the query-text expression that called it. Enforced by
	 * {@code Memory#countOutput}.
	 *
	 * @return the maximum number of values one expression may emit, or {@link Long#MAX_VALUE} for no limit
	 */
	public long getMaxOutputsPerExpression() {
		return maxOutputsPerExpression;
	}

	@Override
	public String toString() {
		return "RuntimeLimits(maxArrayLength=" + maxArrayLength + ", maxObjectMemberCount=" + maxObjectMemberCount + ", maxStringLength=" + maxStringLength + ", maxBinaryLength=" + maxBinaryLength + ", maxUserDefinedFunctionCalls=" + maxUserDefinedFunctionCalls + ", maxOutputsPerExpression=" + maxOutputsPerExpression + ")";
	}
}
