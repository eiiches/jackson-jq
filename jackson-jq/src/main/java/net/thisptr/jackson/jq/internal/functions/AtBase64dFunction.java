package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.exception.JsonQueryException;

@AutoService(Function.class)
@BuiltinFunction(value = "@base64d/0", version = "[1.6, )")
public class AtBase64dFunction<JsonNode> extends AbstractAtFormattingFunction<JsonNode> {
	@Override
	public String convert(final String text) throws JsonQueryException {
		try {
			return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
		} catch (final Throwable th) {
			throw new JsonQueryException(text + " is not valid base64 data: " + th.getMessage());
		}
	}
}
