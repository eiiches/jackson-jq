package net.thisptr.jackson.jq.v2.ext.re2;

import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

final class RuntimeLimitChecks {
	private RuntimeLimitChecks() {
	}

	static void checkStringLength(RuntimeLimits limits, long length) {
		int max = limits.getMaxStringLength();
		if (length > max)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + max);
	}
}
