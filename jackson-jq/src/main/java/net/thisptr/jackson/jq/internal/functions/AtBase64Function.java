package net.thisptr.jackson.jq.internal.functions;

import java.io.UnsupportedEncodingException;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.google.common.io.BaseEncoding;

@BuiltinFunction("@base64/0")
public class AtBase64Function extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		try {
			return BaseEncoding.base64().encode(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
