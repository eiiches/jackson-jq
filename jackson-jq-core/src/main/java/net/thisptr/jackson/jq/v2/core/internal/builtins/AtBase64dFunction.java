package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "@base64d", nargs = 0, version = @VersionRangeSpec(
		min = @VersionSpec(major = 1, minor = 6, patch = 0)
))
public class AtBase64dFunction extends AbstractAtFormattingFunction {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance())));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public String convert(String text) throws JsonQueryException {
		try {
			return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
		} catch (Throwable th) {
			throw new JsonQueryException(text + " is not valid base64 data: " + th.getMessage());
		}
	}
}
