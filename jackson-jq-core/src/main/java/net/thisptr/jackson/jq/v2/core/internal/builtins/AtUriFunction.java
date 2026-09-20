package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * RFC2396
 * <ul>
 * <li>unreserved = alphanum | mark</li>
 * <li>mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"</li>
 * </ul>
 */
@FunctionRegistration(name = "@uri", nargs = 0)
public class AtUriFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text) throws JsonQueryException {
		return URLEncoder.encode(text, StandardCharsets.UTF_8)
				.replaceAll("\\+", "%20")
				.replaceAll("%21", "!")
				.replaceAll("%27", "'")
				.replaceAll("%28", "(")
				.replaceAll("%29", ")")
				.replaceAll("%7E", "~");
	}
}
