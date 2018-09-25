package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

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
