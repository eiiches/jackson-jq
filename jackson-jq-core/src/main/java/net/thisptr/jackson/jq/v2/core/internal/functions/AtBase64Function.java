package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration("@base64/0")
public class AtBase64Function extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text) throws JsonQueryException {
		return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
	}
}
