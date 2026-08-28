package net.thisptr.jackson.jq.v2.spi.exception;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Reports an error that occurred while compiling or evaluating a jq expression.
 */
public class JsonQueryException extends RuntimeException {
	private static final long serialVersionUID = -7241258446595502920L;

	/**
	 * Creates an exception with the given message.
	 *
	 * @param msg the error message
	 */
	public JsonQueryException(String msg) {
		super(msg);
	}

	/**
	 * Creates an exception wrapping the given cause.
	 *
	 * @param e the underlying cause
	 */
	public JsonQueryException(Throwable e) {
		super(e);
	}

	/**
	 * Creates an exception with the given message and cause.
	 *
	 * @param msg the error message
	 * @param rootCause the underlying cause
	 */
	public JsonQueryException(String msg, Throwable rootCause) {
		super(msg, rootCause);
	}

	/**
	 * Converts this exception to the jq value it represents (its message, or {@code null} if none).
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider used to create the result
	 * @return a string node holding {@link #getMessage()}, or a null node if there is no message
	 */
	public <JsonNode> JsonNode toJsonNode(JsonProvider<JsonNode> jsonProvider) {
		String message = getMessage();
		return message == null ? jsonProvider.createNull() : jsonProvider.createString(message);
	}
}
