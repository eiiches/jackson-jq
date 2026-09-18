package net.thisptr.jackson.jq.v2.ext.fs.functions;

import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

final class RuntimeLimitChecks {
	private RuntimeLimitChecks() {
	}

	static void checkArrayLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxArrayLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("Array of " + length + " elements exceeds the maximum array length of " + maximum);
	}

	static void checkObjectLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxObjectMemberCount();
		if (length > maximum)
			throw new RuntimeLimitExceededException("Object of " + length + " members exceeds the maximum object member count of " + maximum);
	}

	static void checkStringLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxStringLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + maximum);
	}

	static int maximumBytesForBase64(int maximumStringLength) {
		return maximumStringLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (maximumStringLength / 4) * 3;
	}
}
