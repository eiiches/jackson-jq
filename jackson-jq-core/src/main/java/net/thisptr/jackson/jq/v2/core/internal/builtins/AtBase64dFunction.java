package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Base64;
import java.util.List;

import com.google.errorprone.annotations.Var;

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
	public String convert(String text, Version version) throws JsonQueryException {
		try {
			return decodeUtf8(Base64.getDecoder().decode(text));
		} catch (Throwable th) {
			throw new JsonQueryException(text + " is not valid base64 data: " + th.getMessage());
		}
	}

	private static String decodeUtf8(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length);
		for (int i = 0; i < bytes.length; ) {
			int first = bytes[i] & 0xff;
			if (first < 0x80) {
				result.append((char) first);
				i++;
				continue;
			}

			int width = first >= 0xc2 && first <= 0xdf ? 2
					: first >= 0xe0 && first <= 0xef ? 3
					: first >= 0xf0 && first <= 0xf4 ? 4 : 1;
			if (width == 1) {
				result.append('\ufffd');
				i++;
				continue;
			}
			if (bytes.length - i < width) {
				result.append('\ufffd');
				break;
			}

			@Var int codePoint = first & (0x7f >> width);
			@Var int consumed = 1;
			while (consumed < width && (bytes[i + consumed] & 0xc0) == 0x80) {
				codePoint = (codePoint << 6) | (bytes[i + consumed] & 0x3f);
				consumed++;
			}
			int minimumCodePoint = width == 2 ? 0x80 : width == 3 ? 0x800 : 0x10000;
			if (consumed == width && codePoint >= minimumCodePoint
					&& (codePoint < 0xd800 || codePoint > 0xdfff) && codePoint <= 0x10ffff) {
				result.appendCodePoint(codePoint);
			} else {
				result.append('\ufffd');
			}
			i += consumed;
		}
		return result.toString();
	}
}
