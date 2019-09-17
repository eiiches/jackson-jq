package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@AutoService(Function.class)
@BuiltinFunction("ltrimstr/1")
public class LTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(final String text, final String trim) {
		if (!text.startsWith(trim))
			return text;
		return text.substring(trim.length());
	}
}
