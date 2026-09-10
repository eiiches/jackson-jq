package net.thisptr.jackson.jq.v2.core.internal.commons.strings;

import java.nio.charset.StandardCharsets;

public class UnicodeUtils {

	public static int lengthUtf8(String text) {
		// TODO: implement without creating an array
		return text.getBytes(StandardCharsets.UTF_8).length;
	}

	public static int lengthUtf32(String in) {
		return in.codePointCount(0, in.length());
	}

	public static String substringUtf32(String in, int begin, int end) {
		int utf16begin = in.offsetByCodePoints(0, begin);
		int utf16end = in.offsetByCodePoints(utf16begin, end - begin);
		return in.substring(utf16begin, utf16end);
	}
}
