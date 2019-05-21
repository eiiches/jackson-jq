package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;
	private static final int MAX_JSON_STRING_LENGTH = 14;

	private static String truncate(final String text, final int len) {
		if (text.length() <= len)
			return text;
		return text.substring(0, len - 3) + "...";
	}

	public JsonQueryTypeException(final JsonNode obj, final String msg) {
		this("%s %s", obj, msg);
	}

	public JsonQueryTypeException(final JsonNode obj1, final JsonNode obj2, final String msg) {
		this("%s and %s %s", obj1, obj2, msg);
	}

	private static String formatJsonNodes(final String format, final Object... args) {
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

	public JsonQueryTypeException(final String format, final Object... args) {
		super(formatJsonNodes(format, args));
	}
}
