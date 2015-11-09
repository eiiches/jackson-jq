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
		super(String.format("%s (%s) %s",
				obj.getNodeType().toString().toLowerCase(),
				truncate(obj.toString(), MAX_JSON_STRING_LENGTH), msg));
	}

	public JsonQueryTypeException(final JsonNode obj1, final JsonNode obj2, final String msg) {
		super(String.format("%s (%s) and %s (%s) %s",
				obj1.getNodeType().toString().toLowerCase(),
				truncate(obj1.toString(), MAX_JSON_STRING_LENGTH),
				obj2.getNodeType().toString().toLowerCase(),
				truncate(obj2.toString(), MAX_JSON_STRING_LENGTH),
				msg));
	}
}
