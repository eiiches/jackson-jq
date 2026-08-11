package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("rtrimstr/1")
public class RTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(final String text, final String trim) {
		if (!text.endsWith(trim))
			return text;
		return text.substring(0, text.length() - trim.length());
	}
}
