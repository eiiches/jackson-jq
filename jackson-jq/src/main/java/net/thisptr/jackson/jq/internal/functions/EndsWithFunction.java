package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

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
