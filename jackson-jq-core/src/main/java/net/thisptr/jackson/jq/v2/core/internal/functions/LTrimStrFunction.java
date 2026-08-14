package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "ltrimstr", nargs = 1)
public class LTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(String text, String trim) {
		if (!text.startsWith(trim))
			return text;
		return text.substring(trim.length());
	}
}
