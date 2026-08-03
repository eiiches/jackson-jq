package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("rtrimstr/1")
public class RTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(final String text, final String trim) {
		if (!text.endsWith(trim))
			return text;
		return text.substring(0, text.length() - trim.length());
	}
}
