package net.thisptr.jackson.jq.v2.spi.type;

/**
 * What a {@link NumericType} knows about the number it describes, beyond it being a number.
 * <p>
 * This is a hint and nothing more: a type is never rejected for the kind it carries, and an
 * expression is free to report {@link #UNKNOWN} rather than work out which of the other two applies.
 * It describes the value, not how a {@code JsonProvider} happens to store it -- an integral value is
 * {@link #INT} whether the provider holds it as an {@code int} or as a {@code BigDecimal}.
 */
public enum NumberKind {
	/**
	 * An integral number, of any magnitude. Nothing here bounds it to what an {@code int} can hold.
	 */
	INT,

	/**
	 * A number that is not integral, including {@code NaN} and the infinities.
	 */
	FLOAT,

	/**
	 * A number whose kind was not worked out.
	 */
	UNKNOWN;
}
