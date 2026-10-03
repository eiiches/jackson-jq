package net.thisptr.jackson.jq.v2.json.internal.io;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Controls how {@link JsonCodec#parse(JsonProvider, String, ParseOptions)} and its siblings hold the
 * JSON numbers they read and bound containers they build.
 * <p>
 * Instances are immutable and can be reused across calls. Build one with {@link #newBuilder()}.
 * Options apply at every depth of the value.
 */
public final class ParseOptions {
	private static final ParseOptions DEFAULT = new ParseOptions(false, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final boolean roundNumbersToDouble;
	private final int maxArrayLength;
	private final int maxObjectMemberCount;

	private ParseOptions(boolean roundNumbersToDouble, int maxArrayLength, int maxObjectMemberCount) {
		this.roundNumbersToDouble = roundNumbersToDouble;
		this.maxArrayLength = maxArrayLength;
		this.maxObjectMemberCount = maxObjectMemberCount;
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
				.setRoundNumbersToDouble(roundNumbersToDouble)
				.setMaxArrayLength(maxArrayLength)
				.setMaxObjectMemberCount(maxObjectMemberCount);
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
	 * Returns the largest array this parser may build.
	 *
	 * @return the maximum array length
	 */
	public int getMaxArrayLength() {
		return maxArrayLength;
	}

	/**
	 * Returns the largest object this parser may build.
	 *
	 * @return the maximum object member count
	 */
	public int getMaxObjectMemberCount() {
		return maxObjectMemberCount;
	}

	/**
	 * Builds a {@link ParseOptions} instance.
	 */
	public static final class Builder {
		private boolean roundNumbersToDouble;
		private int maxArrayLength = Integer.MAX_VALUE;
		private int maxObjectMemberCount = Integer.MAX_VALUE;

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
		 * Sets the maximum array length. The default is unbounded.
		 *
		 * @param maxArrayLength the maximum array length
		 * @return this, for chaining
		 */
		public Builder setMaxArrayLength(int maxArrayLength) {
			if (maxArrayLength < 0)
				throw new IllegalArgumentException("maxArrayLength must not be negative");
			this.maxArrayLength = maxArrayLength;
			return this;
		}

		/**
		 * Sets the maximum object member count. The default is unbounded.
		 *
		 * @param maxObjectMemberCount the maximum object member count
		 * @return this, for chaining
		 */
		public Builder setMaxObjectMemberCount(int maxObjectMemberCount) {
			if (maxObjectMemberCount < 0)
				throw new IllegalArgumentException("maxObjectMemberCount must not be negative");
			this.maxObjectMemberCount = maxObjectMemberCount;
			return this;
		}

		/**
		 * Builds the options, reusing the shared instance for the defaults.
		 *
		 * @return the options
		 */
		public ParseOptions build() {
			if (!roundNumbersToDouble && maxArrayLength == Integer.MAX_VALUE && maxObjectMemberCount == Integer.MAX_VALUE)
				return DEFAULT;
			return new ParseOptions(roundNumbersToDouble, maxArrayLength, maxObjectMemberCount);
		}
	}
}
