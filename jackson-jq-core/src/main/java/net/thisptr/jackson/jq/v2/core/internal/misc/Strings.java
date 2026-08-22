package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class Strings {

	public static String join(String sep, Iterable<String> items) {
		StringBuilder builder = new StringBuilder();
		@Var String s = "";
		for (String item : items) {
			builder.append(s);
			builder.append(item);
			s = sep;
		}
		return builder.toString();
	}

	public static String repeat(String pat, int n) {
		StringBuilder builder = new StringBuilder(pat.length() * n);
		for (int i = 0; i < n; ++i)
			builder.append(pat);
		return builder.toString();
	}

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	public static String[] split(String in, String sep) {
		if (sep.isEmpty()) {
			List<String> result = new ArrayList<>();
			int length = in.length();
			for (int offset = 0; offset < length; ) {
				int codepoint = in.codePointAt(offset);
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

	public static String truncate(String text, int len) {
		if (text.length() <= len)
			return text;
		return text.substring(0, len - 3) + "...";
	}

	public static String truncate(String text, @Nullable Version version) {
		return JsonQueryException.truncate(text, version);
	}
}
