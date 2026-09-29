package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

@FunctionRegistration(name = "strflocaltime", nargs = 1, version = @VersionRangeSpec(min = @VersionSpec(major = 1, minor = 6, patch = 0)))
public class StrFLocalTimeFunction extends AbstractStrFTimeFunction {
	public StrFLocalTimeFunction() {
		super("strflocaltime", true);
	}
}
