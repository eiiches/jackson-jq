package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("ltrimstr/1")
public class LTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(final String text, final String trim) {
		if (!text.startsWith(trim))
			return text;
		return text.substring(trim.length());
	}
}
