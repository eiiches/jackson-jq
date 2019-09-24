package net.thisptr.jackson.jq.internal.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Strings {

	public static String join(final String sep, final Iterable<String> items) {
		final StringBuilder builder = new StringBuilder();
		String s = "";
		for (final String item : items) {
			builder.append(s);
			builder.append(item);
			s = sep;
		}
		return builder.toString();
	}

	public static String repeat(final String pat, final int n) {
		final StringBuilder builder = new StringBuilder(pat.length() * n);
		for (int i = 0; i < n; ++i)
			builder.append(pat);
		return builder.toString();
	}

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	public static String[] split(final String in, final String sep) {
		if (sep.isEmpty()) {
			final List<String> result = new ArrayList<>();
			final int length = in.length();
			for (int offset = 0; offset < length;) {
				final int codepoint = in.codePointAt(offset);
				result.add(new String(Character.toChars(codepoint)));
				offset += Character.charCount(codepoint);
			}
			return result.toArray(EMPTY_STRING_ARRAY);
		} else {
			if (in.isEmpty())
				return EMPTY_STRING_ARRAY;
			return in.split(Pattern.quote(sep), -1);
		}
	}

	public static String truncate(final String text, final int len) {
		if (text.length() <= len)
			return text;
		return text.substring(0, len - 3) + "...";
	}
}
