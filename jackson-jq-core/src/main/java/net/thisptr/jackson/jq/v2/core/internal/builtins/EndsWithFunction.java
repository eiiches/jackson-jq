package net.thisptr.jackson.jq.v2.core.internal.builtins;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "endswith", nargs = 1)
public class EndsWithFunction extends AbstractStartsEndsWithFunction {

	public EndsWithFunction() {
		super("endswith");
	}

	@Override
	protected boolean doCheck(String text, String needle) {
		return text.endsWith(needle);
	}
}
