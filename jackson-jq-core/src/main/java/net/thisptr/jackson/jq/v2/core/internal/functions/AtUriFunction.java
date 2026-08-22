package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * RFC2396
 * <ul>
 * <li>unreserved = alphanum | mark</li>
 * <li>mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"</li>
 * </ul>
 */
@AutoService(Function.class)
@FunctionRegistration(name = "@uri", nargs = 0)
public class AtUriFunction extends AbstractAtFormattingFunction {
	@Override
	// Suppress JdkObsolete because URLEncoder.encode(String, Charset) is not available in Java 8 target.
	@SuppressWarnings("JdkObsolete")
	public String convert(String text) throws JsonQueryException {
		try {
			return URLEncoder.encode(text, StandardCharsets.UTF_8.name())
					.replaceAll("\\+", "%20")
					.replaceAll("%21", "!")
					.replaceAll("%27", "'")
					.replaceAll("%28", "(")
					.replaceAll("%29", ")")
					.replaceAll("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
