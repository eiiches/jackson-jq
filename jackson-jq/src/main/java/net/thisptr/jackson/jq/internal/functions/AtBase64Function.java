package net.thisptr.jackson.jq.internal.functions;

import java.util.Base64;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("@base64/0")
public class AtBase64Function extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}
}
