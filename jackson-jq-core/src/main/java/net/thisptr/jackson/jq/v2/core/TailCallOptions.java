package net.thisptr.jackson.jq.v2.core;

/**
 * Whether the compiler turns a call in tail position into a loop, applied with
 * {@link CompileOptions.Builder#setTailCallOptions(TailCallOptions)}.
 * <p>
 * A jq {@code def} that calls something as the last thing it does -- itself, most of the time -- otherwise
 * costs a chain of Java stack frames per iteration that nothing can reclaim until the whole recursion
 * unwinds, because a callee pushes its values through an {@code Output} the caller supplied. A few hundred
 * iterations exhaust the stack. With this on, such a call runs in a loop at a fixed stack depth instead, and
 * recursion depth is bounded by heap like any other data.
 * <p>
 * It applies where the jq manual says it does: when everything to the left of the call emits at most one
 * value per input. {@code until(. > $n; . + 1)} qualifies; {@code until(cond; (1, 2))} does not, and runs
 * exactly as it did before. Either way the values a query produces, and their order, are unchanged -- this
 * setting exists to isolate the optimization, not to choose between behaviours.
 * <p>
 * Turning it off restores the old stack cost, and with it the {@code "Stack overflow during evaluation"} that
 * used to bound a runaway recursion. It is not a resource limit: see {@link RuntimeOptions} for those, and
 * {@link RuntimeOptions.Builder#setMaxOutputsPerExpression(long)} in particular, which bounds a loop that
 * emits or re-evaluates anything.
 * <p>
 * Instances are immutable, so one can be reused for any number of compilations, including concurrent ones.
 * Build one with {@link #newBuilder()}.
 *
 * @see CompileOptions
 */
public final class TailCallOptions {
	private static final TailCallOptions DEFAULT = new TailCallOptions(true);

	private final boolean enabled;

	private TailCallOptions(boolean enabled) {
		this.enabled = enabled;
	}

	// Package-private: CompileOptions needs an instance for its own default, but callers never do -- they
	// either leave the setting alone or build one.
	static TailCallOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default: tail calls optimized.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Whether a call in tail position is compiled as a loop.
	 *
	 * @return {@code true} if tail calls are optimized
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Builds a {@link TailCallOptions}.
	 */
	public static final class Builder {
		private boolean enabled = true;

		private Builder() {
		}

		/**
		 * Sets whether a call in tail position is compiled as a loop.
		 *
		 * @param enabled {@code false} to compile every call the ordinary way
		 * @return this, for chaining
		 */
		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public TailCallOptions build() {
			return enabled ? DEFAULT : new TailCallOptions(false);
		}
	}
}
