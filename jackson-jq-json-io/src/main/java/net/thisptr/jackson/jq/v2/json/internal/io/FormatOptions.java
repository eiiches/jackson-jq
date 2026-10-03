package net.thisptr.jackson.jq.v2.json.internal.io;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Controls how {@link JsonCodec#format(JsonProvider, Object, FormatOptions)} renders JSON numbers,
 * and how long an output it will build before giving up.
 * <p>
 * Instances are immutable and can be reused across calls. Build one with {@link #newBuilder()}.
 * Number options apply at every depth of the value.
 */
public final class FormatOptions {
	private static final FormatOptions DEFAULT = new FormatOptions(false, false, Integer.MAX_VALUE);

	private final boolean lowerCaseDecimalExponent;
	private final boolean roundNumbersToDouble;
	private final int maxLength;

	private FormatOptions(boolean lowerCaseDecimalExponent, boolean roundNumbersToDouble, int maxLength) {
		this.lowerCaseDecimalExponent = lowerCaseDecimalExponent;
		this.roundNumbersToDouble = roundNumbersToDouble;
		this.maxLength = maxLength;
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
				.setRoundNumbersToDouble(roundNumbersToDouble)
				.setMaxLength(maxLength);
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
	 * Returns the longest output formatting will build before it throws
	 * {@link JsonSizeExceededException}.
	 *
	 * @return the maximum output length in UTF-16 code units; {@link Integer#MAX_VALUE} for no limit
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Builds a {@link FormatOptions} instance.
	 */
	public static final class Builder {
		private boolean lowerCaseDecimalExponent;
		private boolean roundNumbersToDouble;
		private int maxLength = Integer.MAX_VALUE;

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
		 * Sets the longest output formatting may build. Formatting stops with
		 * {@link JsonSizeExceededException} as soon as the next piece would carry the output past the
		 * cap, so an output too large to want is never built in full.
		 *
		 * @param maxLength the maximum output length in UTF-16 code units; {@link Integer#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxLength} is negative
		 */
		public Builder setMaxLength(int maxLength) {
			if (maxLength < 0)
				throw new IllegalArgumentException("maxLength must not be negative");
			this.maxLength = maxLength;
			return this;
		}

		/**
		 * Builds the options, reusing the shared instance for the defaults.
		 *
		 * @return the options
		 */
		public FormatOptions build() {
			if (!lowerCaseDecimalExponent && !roundNumbersToDouble && maxLength == Integer.MAX_VALUE)
				return DEFAULT;
			return new FormatOptions(lowerCaseDecimalExponent, roundNumbersToDouble, maxLength);
		}
	}
}
