package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

@FunctionRegistration(name = "localtime", nargs = 0, version = @VersionRangeSpec(min = @VersionSpec(major = 1, minor = 6, patch = 0)))
public class LocalTimeFunction extends AbstractTimeSplitFunction {
	public LocalTimeFunction() {
		super("localtime", true);
	}
}
