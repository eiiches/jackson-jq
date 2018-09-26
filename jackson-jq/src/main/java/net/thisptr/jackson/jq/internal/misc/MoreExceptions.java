package net.thisptr.jackson.jq.internal.misc;

import com.fasterxml.jackson.databind.JsonNode;

public class MoreExceptions {
	private static final int MAX_JSON_STRING_LENGTH = 14;

	private static String truncate(final String text, final int len) {
		if (text.length() <= len)
			return text;
		return text.substring(0, len - 3) + "...";
	}

	public static String format(final String format, final Object... args) {
		final Object[] formattedArguments = new Object[args.length];
		for (int i = 0; i < args.length; ++i) {
			if (args[i] instanceof JsonNode) {
				final JsonNode node = (JsonNode) args[i];
				formattedArguments[i] = String.format("%s (%s)", node.getNodeType().toString().toLowerCase(), truncate(node.toString(), MAX_JSON_STRING_LENGTH));
			} else {
				formattedArguments[i] = args[i];
			}
		}
		return String.format(format, formattedArguments);
	}
}
