package net.thisptr.jackson.jq.v2.json.internal.io;

import java.io.Serial;

/**
 * Reports that a formatted string or parsed container would exceed its configured size limit.
 */
public final class JsonSizeExceededException extends JsonException {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Identifies the value whose size limit was exceeded.
	 */
	public enum Kind {
		STRING,
		ARRAY,
		OBJECT
	}

	private final Kind kind;
	private final long size;

	/**
	 * Creates an exception for a value that would have exceeded its size limit.
	 *
	 * @param kind the kind of value
	 * @param size the size the value would have reached
	 * @param maximum the limit that would have been exceeded
	 */
	public JsonSizeExceededException(Kind kind, long size, int maximum) {
		super(message(kind, size, maximum));
		this.kind = kind;
		this.size = size;
	}

	private static String message(Kind kind, long size, int maximum) {
		return switch (kind) {
			case STRING -> "JSON of " + size + " characters exceeds the maximum length of " + maximum;
			case ARRAY -> "Array of " + size + " elements exceeds the maximum size of " + maximum;
			case OBJECT -> "Object of " + size + " members exceeds the maximum size of " + maximum;
		};
	}

	/**
	 * Returns the kind of value that exceeded its limit.
	 *
	 * @return the value kind
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * Returns the size the value would have reached.
	 *
	 * @return the rejected size in UTF-16 code units, elements, or members, according to {@link #getKind()}
	 */
	public long getSize() {
		return size;
	}
}
