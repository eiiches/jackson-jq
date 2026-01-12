package net.thisptr.jackson.jq.exception;

import net.thisptr.jackson.jq.JsonProvider;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryTypeException(final String msg) {
		super(msg);
	}

	/**
	 * Simple format constructor without JsonProvider - uses default Object.toString() for arguments.
	 */
	public JsonQueryTypeException(final String format, final Object... args) {
		super(String.format(format, args));
	}

	public JsonQueryTypeException(final JsonProvider<?> jsonProvider, final String format, final Object... args) {
		super(jsonProvider, format, args);
	}
}
