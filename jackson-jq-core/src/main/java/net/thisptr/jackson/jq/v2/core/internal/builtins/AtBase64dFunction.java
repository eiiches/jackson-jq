package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "@base64d", nargs = 0, version = @VersionRangeSpec(
		min = @VersionSpec(major = 1, minor = 6, patch = 0)
))
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
