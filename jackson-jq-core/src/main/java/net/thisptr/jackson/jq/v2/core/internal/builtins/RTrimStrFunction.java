package net.thisptr.jackson.jq.v2.core.internal.builtins;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

@FunctionRegistration(name = "rtrimstr", nargs = 1, version = @VersionRangeSpec(
		max = @VersionSpec(major = 1, minor = 8, patch = 0)
))
public class RTrimStrFunction extends AbstractTrimStrFunction {
	@Override
	protected String doTrim(String text, String trim) {
		if (!text.endsWith(trim))
			return text;
		return text.substring(0, text.length() - trim.length());
	}
}
