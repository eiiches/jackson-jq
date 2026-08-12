package net.thisptr.jackson.jq.v2.regex.impl.joni;
import com.google.errorprone.annotations.Var;

final class UnicodeUtils {
	private UnicodeUtils() {}

	static int utf8CharLength(byte ch) {
		if ((ch & 0b10000000) == 0b00000000)
			return 1;
		if ((ch & 0b11100000) == 0b11000000)
			return 2;
		if ((ch & 0b11110000) == 0b11100000)
			return 3;
		if ((ch & 0b11111000) == 0b11110000)
			return 4;
		if ((ch & 0b11111100) == 0b11111000)
			return 5;
		if ((ch & 0b11111110) == 0b11111100)
			return 6;
		if ((ch & 0b11000000) == 0b10000000)
			throw new IllegalArgumentException(String.format("This is not a first byte of a Unicode character: %x", ch));
		if ((ch & 0xff) == 0xfe || (ch & 0xff) == 0xff)
			throw new IllegalArgumentException(String.format("This is part of a byte order mark (BOM): %x", ch));
		throw new IllegalArgumentException(String.format("This is an unknown UTF-8 byte: %x", ch));
	}

	static int[] utf8CharIndex(byte[] bytes) {
		int[] result = new int[bytes.length + 1];
		@Var int utf8Index = 0;
		@Var int codePointIndex = 0;
		while (utf8Index < bytes.length) {
			int charLength = utf8CharLength(bytes[utf8Index]);
			for (int i = 0; i < charLength; ++i)
				result[utf8Index + i] = codePointIndex;
			++codePointIndex;
			utf8Index += charLength;
		}
		result[bytes.length] = codePointIndex;
		return result;
	}
}
