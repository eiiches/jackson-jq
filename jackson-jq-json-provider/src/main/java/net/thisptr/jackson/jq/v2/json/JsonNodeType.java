package net.thisptr.jackson.jq.v2.json;

/**
 * The category a {@link JsonProvider} classifies a node into, as reported by
 * {@link JsonProvider#getNodeType}.
 * <p>
 * Every constant except {@link #BINARY} corresponds to one of the six JSON types.
 */
public enum JsonNodeType {
	/**
	 * A JSON object.
	 */
	OBJECT,

	/**
	 * A JSON array.
	 */
	ARRAY,

	/**
	 * A JSON string.
	 */
	STRING,

	/**
	 * A JSON number.
	 * <p>
	 * Which Java type holds the value is a separate question, answered by
	 * {@link JsonProvider#getNumberType}.
	 */
	NUMBER,

	/**
	 * A JSON boolean.
	 */
	BOOLEAN,

	/**
	 * A JSON null.
	 */
	NULL,

	/**
	 * A binary node holding raw bytes.
	 * <p>
	 * Binary is not a JSON type. Only providers whose underlying library has such a node ever report
	 * it; the rest never produce this constant, and their {@link JsonProvider#createBinary} throws
	 * {@link UnsupportedOperationException}.
	 */
	BINARY;
}
