package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("startswith/1")
public class StartsWithFunction extends AbstractStartsEndsWithFunction {
	public StartsWithFunction() {
		super("startswith");
	}

	@Override
	protected boolean doCheck(final String text, final String needle) {
		return text.startsWith(needle);
	}
}
