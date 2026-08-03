package net.thisptr.jackson.jq.regex.joni;

final class UnicodeUtils {
	private UnicodeUtils() {}

	static int UTF8CharLength(final byte ch) {
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

	static int[] UTF8CharIndex(final byte[] bytes) {
		final int[] r = new int[bytes.length + 1];

		int iUtf8 = 0;
		int iCodepoint = 0;

		while (iUtf8 < bytes.length) {
			final int charLen = UTF8CharLength(bytes[iUtf8]);
			for (int i = 0; i < charLen; ++i)
				r[iUtf8 + i] = iCodepoint;
			iCodepoint += 1;
			iUtf8 += charLen;
		}
		r[bytes.length] = iCodepoint;

		return r;
	}
}
