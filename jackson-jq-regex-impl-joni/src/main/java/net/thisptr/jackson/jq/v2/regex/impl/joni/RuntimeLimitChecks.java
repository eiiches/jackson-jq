package net.thisptr.jackson.jq.v2.regex.impl.joni;

import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

/**
 * The subset of the engine's limit checks this module needs. Duplicated rather than shared because
 * the checks live in a core-internal package and the SPI is deliberately kept minimal.
 */
final class RuntimeLimitChecks {
	private RuntimeLimitChecks() {
	}

	/**
	 * Verifies that a string of {@code length} UTF-16 code units is within {@code limits}. Callers
	 * must check before allocating, not after filling.
	 */
	static void checkStringLength(RuntimeLimits limits, long length) {
		int max = limits.getMaxStringLength();
		if (length > max)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + max);
	}
}
