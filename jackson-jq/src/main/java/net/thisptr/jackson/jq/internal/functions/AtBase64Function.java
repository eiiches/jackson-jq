package net.thisptr.jackson.jq.internal.functions;

import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@AutoService(Function.class)
@BuiltinFunction("@base64/0")
public class AtBase64Function extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}
}
