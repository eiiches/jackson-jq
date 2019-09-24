package net.thisptr.jackson.jq.internal.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

/**
 * RFC2396
 * <ul>
 * <li>unreserved = alphanum | mark</li>
 * <li>mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"</li>
 * </ul>
 */
@AutoService(Function.class)
@BuiltinFunction("@uri/0")
public class AtUriFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		try {
			return URLEncoder.encode(text, "UTF-8")
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
