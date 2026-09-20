package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.Map;

import com.google.errorprone.annotations.Var;
import com.google.re2j.Pattern;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

final class Re2Pattern {
	final Pattern pattern;
	final boolean global;
	final @Nullable String[] names;

	Re2Pattern(String regexText, @Nullable String flags) throws JsonQueryException {
		pattern = Pattern.compile(regexText, parseModifiers(flags));
		global = flags != null && flags.indexOf('g') >= 0;
		names = names(pattern);
	}

	private static @Nullable String[] names(Pattern pattern) {
		String[] result = new String[pattern.groupCount() + 1];
		for (Map.Entry<String, Integer> entry : pattern.namedGroups().entrySet())
			result[entry.getValue()] = entry.getKey();
		return result;
	}

	private static int parseModifiers(@Nullable String flags) throws JsonQueryException {
		if (flags == null)
			return 0;

		@Var int result = 0;
		for (int i = 0; i < flags.length(); i++) {
			switch (flags.charAt(i)) {
				case 'g':
				case 's':
					break;
				case 'i':
					result |= Pattern.CASE_INSENSITIVE;
					break;
				case 'm', 'p':
					result |= Pattern.DOTALL;
					break;
				case 'l':
					result |= Pattern.LONGEST_MATCH;
					break;
				default:
					throw new JsonQueryException(String.format("%s is not a valid modifier string", flags));
			}
		}
		return result;
	}
}
