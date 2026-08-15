package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
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
