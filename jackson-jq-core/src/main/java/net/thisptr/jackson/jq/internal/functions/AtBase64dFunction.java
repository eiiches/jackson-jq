package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.exception.JsonQueryException;

@AutoService(Function.class)
@BuiltinFunction(value = "@base64d/0", version = "[1.6, )")
public class AtBase64dFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) throws JsonQueryException {
		try {
			return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
		} catch (final Throwable th) {
			throw new JsonQueryException("%s is not valid base64 data: %s", TextNode.valueOf(text), th.getMessage());
		}
	}
}
