package net.thisptr.jackson.jq.v2.spi;

/**
 * The number of values an expression is known to emit for one input on normal completion.
 */
public enum Cardinality {
	/**
	 * The expression emits no values.
	 */
	ZERO,
	/**
	 * The expression emits exactly one value.
	 */
	ONE,
	/**
	 * The number of emitted values is not known to be zero or one.
	 */
	UNKNOWN,
}
