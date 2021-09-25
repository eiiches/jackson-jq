package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("startswith/1")
public class StartsWithFunction extends AbstractStartsEndsWithFunction {
	public StartsWithFunction() {
		super("startswith");
	}

	@Override
	protected boolean doCheck(final String text, final String needle) {
		return text.startsWith(needle);
	}
}
