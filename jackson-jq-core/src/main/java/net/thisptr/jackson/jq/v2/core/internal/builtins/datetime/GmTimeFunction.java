package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "gmtime", nargs = 0)
public class GmTimeFunction extends AbstractTimeSplitFunction {
	public GmTimeFunction() {
		super("gmtime", false);
	}
}
