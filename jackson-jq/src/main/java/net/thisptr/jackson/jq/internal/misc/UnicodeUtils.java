package net.thisptr.jackson.jq.internal.misc;

import java.nio.charset.StandardCharsets;

public class UnicodeUtils {

	public static int UTF8CharLength(final byte ch) {
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
			throw new IllegalArgumentException(String.format("This is not a first byte of unicode charactor: %x", ch));
		if ((ch & 0xff) == 0xfe || (ch & 0xff) == 0xff)
			throw new IllegalArgumentException(String.format("This is a part of a byte order mark (BOM): %x", ch));
		throw new IllegalArgumentException(String.format("This is an unknown UTF-8 byte: %x", ch));
	}

	public static int[] UTF8CharIndex(final byte[] bytes) {
		final int[] r = new int[bytes.length + 1];

		int i_utf8 = 0;
		int i_codepoint = 0;

		while (i_utf8 < bytes.length) {
			final int charLen = UTF8CharLength(bytes[i_utf8]);
			for (int i = 0; i < charLen; ++i)
				r[i_utf8 + i] = i_codepoint;
			i_codepoint += 1;
			i_utf8 += charLen;
		}
		r[bytes.length] = i_codepoint;

		return r;
	}

	public static int lengthUtf8(final String text) {
		// TODO: implement without creating an array
		return text.getBytes(StandardCharsets.UTF_8).length;
	}

	public static int lengthUtf32(final String in) {
		return in.codePointCount(0, in.length());
	}

	public static String substringUtf32(final String in, final int begin, final int end) {
		final int utf16begin = in.offsetByCodePoints(0, begin);
		final int utf16end = in.offsetByCodePoints(utf16begin, end - begin);
		return in.substring(utf16begin, utf16end);
	}
}
