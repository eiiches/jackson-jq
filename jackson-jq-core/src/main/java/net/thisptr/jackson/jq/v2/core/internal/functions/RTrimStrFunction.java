package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "rtrimstr", nargs = 1)
public class RTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(String text, String trim) {
		if (!text.endsWith(trim))
			return text;
		return text.substring(0, text.length() - trim.length());
	}
}
