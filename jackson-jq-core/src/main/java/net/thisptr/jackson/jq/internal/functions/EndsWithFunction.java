package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("endswith/1")
public class EndsWithFunction extends AbstractStartsEndsWithFunction {

	public EndsWithFunction() {
		super("endswith");
	}

	@Override
	protected boolean doCheck(final String text, final String needle) {
		return text.endsWith(needle);
	}
}
