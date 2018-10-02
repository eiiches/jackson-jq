package net.thisptr.jackson.jq.exception;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryTypeException(final String msg) {
		super(msg);
	}

	public JsonQueryTypeException(final String format, final Object... args) {
		super(format, args);
	}
}
