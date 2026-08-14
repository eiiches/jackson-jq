package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "startswith", nargs = 1)
public class StartsWithFunction extends AbstractStartsEndsWithFunction {
	public StartsWithFunction() {
		super("startswith");
	}

	@Override
	protected boolean doCheck(String text, String needle) {
		return text.startsWith(needle);
	}
}
