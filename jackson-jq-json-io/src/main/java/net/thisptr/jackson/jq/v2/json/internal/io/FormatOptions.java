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

	private final boolean lowerCaseExponent;
	private final boolean roundNumbersToDouble;

	private FormatOptions(boolean lowerCaseExponent, boolean roundNumbersToDouble) {
		this.lowerCaseExponent = lowerCaseExponent;
		this.roundNumbersToDouble = roundNumbersToDouble;
	}

	static FormatOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder that preserves exact decimal values and uses uppercase exponents.
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
				.setLowerCaseExponent(lowerCaseExponent)
				.setRoundNumbersToDouble(roundNumbersToDouble);
	}

	/**
	 * Returns whether number exponents use {@code e} instead of {@code E}.
	 *
	 * @return whether exponents are lowercase
	 */
	public boolean getLowerCaseExponent() {
		return lowerCaseExponent;
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
		private boolean lowerCaseExponent;
		private boolean roundNumbersToDouble;

		private Builder() {
		}

		/**
		 * Sets whether number exponents use {@code e} instead of {@code E}.
		 *
		 * @param lowerCaseExponent whether exponents are lowercase
		 * @return this, for chaining
		 */
		public Builder setLowerCaseExponent(boolean lowerCaseExponent) {
			this.lowerCaseExponent = lowerCaseExponent;
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
			if (!lowerCaseExponent && !roundNumbersToDouble)
				return DEFAULT;
			return new FormatOptions(lowerCaseExponent, roundNumbersToDouble);
		}
	}
}
