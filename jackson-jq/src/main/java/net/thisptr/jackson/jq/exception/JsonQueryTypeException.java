package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.misc.MoreExceptions;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryTypeException(final JsonNode obj, final String msg) {
		this("%s %s", obj, msg);
	}

	public JsonQueryTypeException(final JsonNode obj1, final JsonNode obj2, final String msg) {
		this("%s and %s %s", obj1, obj2, msg);
	}

	public JsonQueryTypeException(final String format, final Object... args) {
		super(MoreExceptions.format(format, args));
	}
}
