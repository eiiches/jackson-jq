package net.thisptr.jackson.jq.v2.json;

/**
 * Reports a failure to parse or otherwise interpret raw JSON text via a {@link JsonProvider}.
 */
public class JsonException extends RuntimeException {
	private static final long serialVersionUID = 5533862584931897206L;

	/**
	 * Creates an exception with the given message.
	 *
	 * @param msg the error message
	 */
	public JsonException(String msg) {
		super(msg);
	}

	/**
	 * Creates an exception wrapping the given cause.
	 *
	 * @param e the underlying cause
	 */
	public JsonException(Throwable e) {
		super(e);
	}

	/**
	 * Creates an exception with the given message and cause.
	 *
	 * @param msg the error message
	 * @param rootCause the underlying cause
	 */
	public JsonException(String msg, Throwable rootCause) {
		super(msg, rootCause);
	}
}
