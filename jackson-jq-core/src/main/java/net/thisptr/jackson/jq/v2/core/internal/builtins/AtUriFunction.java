package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "@uri", nargs = 0)
public class AtUriFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text, Version version) {
		String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8)
				.replace("+", "%20")
				.replace("%7E", "~");
		if (version.compareTo(Versions.JQ_1_7) < 0) {
			return encoded.replace("%21", "!")
					.replace("%27", "'")
					.replace("%28", "(")
					.replace("%29", ")");
		}
		return encoded.replace("*", "%2A");
	}
}
