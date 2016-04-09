package net.thisptr.jackson.jq.internal.misc;

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

	public static String[] splitToArray(final String text, final String sep) {
		return text.split(Pattern.quote(sep), -1);
	}

	public static String[] splitToArray(final String text, final Pattern sep) {
		return sep.split(text, -1);
	}
}
