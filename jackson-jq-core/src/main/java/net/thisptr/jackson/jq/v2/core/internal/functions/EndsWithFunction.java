package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
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
