package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("split2/1")
public class Split2Function extends SplitFunction {
	public Split2Function() {
		super("split2");
	}

	@Override
	protected String[] split(final String in, final String sep) {
		return in.split(sep);
	}
}
