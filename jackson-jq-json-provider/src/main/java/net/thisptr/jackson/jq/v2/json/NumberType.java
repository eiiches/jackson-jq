package net.thisptr.jackson.jq.v2.json;

/**
 * The Java type a {@link JsonProvider} uses to represent a JSON number.
 * <p>
 * This describes the representation the underlying JSON library happens to hold, not the logical
 * value: the same number can be reported differently by different providers, and a provider is free
 * to report {@link #UNKNOWN} for values whose representation it does not track.
 */
public enum NumberType {
	/**
	 * The value is held as an {@code int} (or a narrower integral type).
	 */
	INT,

	/**
	 * The value is held as a {@code long}.
	 */
	LONG,

	/**
	 * The value is held as a {@link java.math.BigInteger}.
	 */
	BIG_INTEGER,

	/**
	 * The value is held as a {@link java.math.BigDecimal}.
	 */
	BIG_DECIMAL,

	/**
	 * The value is held as a {@code double}.
	 */
	DOUBLE,

	/** The value is held as a {@code float}. */
	FLOAT,

	/**
	 * The value is a number, but the provider does not track which of the above represents it.
	 * <p>
	 * Gson reports this for numbers coming out of its parser, which are held as a
	 * {@code LazilyParsedNumber} until something asks for a specific type.
	 */
	UNKNOWN;
}
