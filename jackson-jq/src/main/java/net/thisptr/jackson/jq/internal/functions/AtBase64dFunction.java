package net.thisptr.jackson.jq.internal.functions;

import java.util.Base64;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("@base64d/0")
public class AtBase64dFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		return new String(Base64.getDecoder().decode(text));
	}
}
