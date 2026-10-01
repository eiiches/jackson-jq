package net.thisptr.jackson.jq.v2.json.internal.io;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Controls how {@link JsonCodec#parse(JsonProvider, String, ParseOptions)} and its siblings hold the
 * JSON numbers they read.
 * <p>
 * Instances are immutable and can be reused across calls. Build one with {@link #newBuilder()}.
 * Options apply to numbers at every depth of the value.
 */
public final class ParseOptions {
	private static final ParseOptions DEFAULT = new ParseOptions(false);

	private final boolean roundNumbersToDouble;

	private ParseOptions(boolean roundNumbersToDouble) {
		this.roundNumbersToDouble = roundNumbersToDouble;
	}

	static ParseOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder that keeps the exact decimal value of every number it reads.
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
				.setRoundNumbersToDouble(roundNumbersToDouble);
	}

	/**
	 * Returns whether numbers are rounded to {@code double} as they are read, rather than kept as the
	 * exact decimal they were written as.
	 *
	 * @return whether numbers are rounded to {@code double}
	 */
	public boolean getRoundNumbersToDouble() {
		return roundNumbersToDouble;
	}

	/**
	 * Builds a {@link ParseOptions} instance.
	 */
	public static final class Builder {
		private boolean roundNumbersToDouble;

		private Builder() {
		}

		/**
		 * Sets whether numbers are rounded to {@code double} as they are read. jq held every number as a
		 * {@code double} before 1.7, so two literals that round to the same {@code double} are one value
		 * there -- 9007199254740993 and 9007199254740992 compare equal, and {@code unique} keeps one of
		 * them. Rounding only on output would get the printing right and those comparisons wrong.
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
		public ParseOptions build() {
			if (!roundNumbersToDouble)
				return DEFAULT;
			return new ParseOptions(roundNumbersToDouble);
		}
	}
}
