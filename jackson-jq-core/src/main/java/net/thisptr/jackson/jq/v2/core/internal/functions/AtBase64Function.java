package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "@base64", nargs = 0)
public class AtBase64Function extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text) throws JsonQueryException {
		return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
	}
}
