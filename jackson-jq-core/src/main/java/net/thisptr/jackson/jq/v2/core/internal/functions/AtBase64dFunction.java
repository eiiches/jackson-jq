package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(value = "@base64d/0", version = "[1.6, )")
public class AtBase64dFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text) throws JsonQueryException {
		try {
			return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
		} catch (Throwable th) {
			throw new JsonQueryException(text + " is not valid base64 data: " + th.getMessage());
		}
	}
}
