package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

@AutoService(Function.class)
@FunctionRegistration(name = "ltrimstr", nargs = 1, version = @VersionRangeSpec(
		max = @VersionSpec(major = 1, minor = 8, patch = 0)
))
public class LTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(String text, String trim) {
		if (!text.startsWith(trim))
			return text;
		return text.substring(trim.length());
	}
}
