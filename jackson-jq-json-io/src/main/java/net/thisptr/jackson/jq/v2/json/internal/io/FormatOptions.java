package net.thisptr.jackson.jq.v2.json.internal.io;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Controls how {@link JsonCodec#format(JsonProvider, Object, FormatOptions)} renders JSON numbers.
 * <p>
 * Instances are immutable and can be reused across calls. Build one with {@link #newBuilder()}.
 * Options apply to numbers at every depth of the value.
 */
public final class FormatOptions {
	private static final FormatOptions DEFAULT = new FormatOptions(false, false);

	private final boolean lowerCaseDecimalExponent;
	private final boolean roundNumbersToDouble;

	private FormatOptions(boolean lowerCaseDecimalExponent, boolean roundNumbersToDouble) {
		this.lowerCaseDecimalExponent = lowerCaseDecimalExponent;
		this.roundNumbersToDouble = roundNumbersToDouble;
	}

	static FormatOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder that preserves exact decimal values and their exponent case.
	 * Computed floating-point values use jq-style lowercase exponents.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Creates a builder holding this instance's settings.
	 *
	 * @return a new builder
	 */
	public Builder toBuilder() {
		return new Builder()
				.setLowerCaseDecimalExponent(lowerCaseDecimalExponent)
				.setRoundNumbersToDouble(roundNumbersToDouble);
	}

	/**
	 * Returns whether exponents of values formatted from {@code BigDecimal} use {@code e} instead of {@code E}.
	 * Computed floating-point exponents always use {@code e}.
	 *
	 * @return whether decimal exponents are lowercase
	 */
	public boolean getLowerCaseDecimalExponent() {
		return lowerCaseDecimalExponent;
	}

	/**
	 * Returns whether numbers are rounded to {@code double} before formatting.
	 *
	 * @return whether numbers are rounded to {@code double}
	 */
	public boolean getRoundNumbersToDouble() {
		return roundNumbersToDouble;
	}

	/**
	 * Builds a {@link FormatOptions} instance.
	 */
	public static final class Builder {
		private boolean lowerCaseDecimalExponent;
		private boolean roundNumbersToDouble;

		private Builder() {
		}

		/**
		 * Sets whether exponents of values formatted from {@code BigDecimal} use {@code e} instead of {@code E}.
		 * Computed floating-point exponents always use {@code e}.
		 *
		 * @param lowerCaseDecimalExponent whether decimal exponents are lowercase
		 * @return this, for chaining
		 */
		public Builder setLowerCaseDecimalExponent(boolean lowerCaseDecimalExponent) {
			this.lowerCaseDecimalExponent = lowerCaseDecimalExponent;
			return this;
		}

		/**
		 * Sets whether numbers are rounded to {@code double} before formatting.
		 *
		 * @param roundNumbersToDouble whether to round numbers to {@code double}
		 * @return this, for chaining
		 */
		public Builder setRoundNumbersToDouble(boolean roundNumbersToDouble) {
			this.roundNumbersToDouble = roundNumbersToDouble;
			return this;
		}

		/**
		 * Builds the options, reusing the shared instance for the defaults.
		 *
		 * @return the options
		 */
		public FormatOptions build() {
			if (!lowerCaseDecimalExponent && !roundNumbersToDouble)
				return DEFAULT;
			return new FormatOptions(lowerCaseDecimalExponent, roundNumbersToDouble);
		}
	}
}
