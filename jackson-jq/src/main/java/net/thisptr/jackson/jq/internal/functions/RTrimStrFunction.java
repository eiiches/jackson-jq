package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("rtrimstr/1")
public class RTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(final String text, final String trim) {
		if (!text.endsWith(trim))
			return text;
		return text.substring(0, text.length() - trim.length());
	}
}
