package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

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

	private static final byte INVALID = -1;

	private static final byte[] DECODE_TABLE = new byte[128];

	static {
		Arrays.fill(DECODE_TABLE, INVALID);
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
		for (int i = 0; i < alphabet.length(); ++i)
			DECODE_TABLE[alphabet.charAt(i)] = (byte) i;
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public String convert(String text, Version version, UnaryOperator<String> describe) throws JsonQueryException {
		return decodeUtf8(decodeBase64(text, describe));
	}

	/**
	 * Decodes base64 the way jq decodes it, which is not the way RFC 4648 defines it.
	 * <p>
	 * jq walks the input a byte at a time and stops at the first {@code =}, discarding the rest of the
	 * input rather than checking that the padding is the canonical one, so {@code "YWJjZA=X"} and
	 * {@code "YQ==YQ=="} decode instead of failing. Only the standard alphabet is accepted: the
	 * URL-safe {@code -} and {@code _}, whitespace and every other byte are invalid. Iterating over
	 * {@code char}s rather than UTF-8 bytes makes no difference, since anything outside US-ASCII --
	 * an unpaired surrogate included -- is invalid either way.
	 *
	 * @param text the input, already converted to a string the way jq converts it
	 * @param describe renders the input the way jq names it in the error
	 * @return the decoded bytes, which are not necessarily valid UTF-8
	 * @throws JsonQueryException if the input holds a character outside the alphabet, or ends with a
	 * group of a single character, which carries too few bits to make a byte
	 */
	private static byte[] decodeBase64(String text, UnaryOperator<String> describe) throws JsonQueryException {
		// 4 characters make 3 bytes, 3 make 2 and 2 make 1, so this bound holds for every leftover size.
		byte[] result = new byte[text.length() / 4 * 3 + 2];
		@Var int size = 0;
		@Var int pending = 0;
		@Var int code = 0;
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
			if (ch == '=')
				break;
			int value = ch < DECODE_TABLE.length ? DECODE_TABLE[ch] : INVALID;
			if (value == INVALID)
				throw new JsonQueryException(describe.apply(text) + " is not valid base64 data");
			code = (code << 6) | value;
			if (++pending == 4) {
				result[size++] = (byte) (code >> 16);
				result[size++] = (byte) (code >> 8);
				result[size++] = (byte) code;
				pending = 0;
				code = 0;
			}
		}
		if (pending == 3) {
			result[size++] = (byte) (code >> 10);
			result[size++] = (byte) (code >> 2);
		} else if (pending == 2) {
			result[size++] = (byte) (code >> 4);
		} else if (pending == 1) {
			throw new JsonQueryException(describe.apply(text) + " trailing base64 byte found");
		}
		return Arrays.copyOf(result, size);
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
