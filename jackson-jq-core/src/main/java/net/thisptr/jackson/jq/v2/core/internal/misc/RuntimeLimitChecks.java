package net.thisptr.jackson.jq.v2.core.internal.misc;

import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

/**
 * Guards the points where evaluation can grow a value beyond the size its inputs already had --
 * array/object construction, {@code +} and {@code *}, the {@code setpath} family, and everything
 * that builds a new string.
 * <p>
 * Callers must check <em>before</em> allocating, not after filling: for
 * {@code setpath([100000000]; 1)} the preallocation alone is what exhausts the heap.
 * <p>
 * The user-defined function call budget is the odd one out -- it counts rather than sizes, so it needs
 * per-invocation state and is tallied by {@code Memory#countUserDefinedFunctionCall}. Only its message
 * lives here, so that every limit still words its failure the same way.
 */
public final class RuntimeLimitChecks {

	private RuntimeLimitChecks() {
	}

	/**
	 * Verifies that an array of {@code size} elements is within {@code limits}.
	 *
	 * @param limits the invocation's limits
	 * @param size the number of elements the array being built would have
	 * @throws RuntimeLimitExceededException if {@code size} exceeds {@link RuntimeLimits#getMaxArrayLength()}
	 */
	public static void checkArraySize(RuntimeLimits limits, long size) {
		int max = limits.getMaxArrayLength();
		if (size > max)
			throw new RuntimeLimitExceededException("Array of " + size + " elements exceeds the maximum array size of " + max);
	}

	/**
	 * Verifies that an object of {@code size} members is within {@code limits}.
	 *
	 * @param limits the invocation's limits
	 * @param size the number of members the object being built would have
	 * @throws RuntimeLimitExceededException if {@code size} exceeds {@link RuntimeLimits#getMaxObjectMemberCount()}
	 */
	public static void checkObjectSize(RuntimeLimits limits, long size) {
		int max = limits.getMaxObjectMemberCount();
		if (size > max)
			throw new RuntimeLimitExceededException("Object of " + size + " members exceeds the maximum object size of " + max);
	}

	/**
	 * Verifies that a string of {@code length} UTF-16 code units is within {@code limits}.
	 * <p>
	 * With no limit configured the maximum is {@link Integer#MAX_VALUE}, so passing the length as a
	 * {@code long} also rejects anything {@link String} could not represent in the first place.
	 *
	 * @param limits the invocation's limits
	 * @param length the number of {@code char}s the string being built would have
	 * @throws RuntimeLimitExceededException if {@code length} exceeds {@link RuntimeLimits#getMaxStringLength()}
	 */
	public static void checkStringLength(RuntimeLimits limits, long length) {
		int max = limits.getMaxStringLength();
		if (length > max)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + max);
	}

	/**
	 * Builds the failure for a breached user-defined function call budget.
	 * <p>
	 * Returns the exception instead of throwing it so the counting itself stays on {@code Memory}, where the
	 * per-invocation tally lives, and the caller's {@code throw} keeps the breach visibly terminal.
	 *
	 * @param max the budget that was exceeded
	 * @return the exception to throw
	 */
	public static RuntimeLimitExceededException userDefinedFunctionCallsExceeded(long max) {
		return new RuntimeLimitExceededException("Query exceeds the maximum of " + max + " user-defined function calls");
	}
}
