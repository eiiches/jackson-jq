package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "strftime", nargs = 1)
public class StrFTimeFunction extends AbstractStrFTimeFunction {
	public StrFTimeFunction() {
		super("strftime", false);
	}
}
