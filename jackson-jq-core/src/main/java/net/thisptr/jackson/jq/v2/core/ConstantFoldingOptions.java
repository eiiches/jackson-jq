package net.thisptr.jackson.jq.v2.core;

import java.util.Objects;

/**
 * How much the compiler may evaluate while compiling, applied with
 * {@link OptimizationOptions.Builder#setConstantFoldingOptions(ConstantFoldingOptions)}.
 * <p>
 * An expression that depends on neither the input, nor external state, nor a variable has the same result
 * every time, so the compiler evaluates it once, while compiling, and the compiled query emits the values it
 * found instead of running the expression again. These settings say how much work that is worth, and what
 * happens past it is never a failure: a fold that would exceed one of them is abandoned, and the expression
 * stays on the evaluation path exactly as it would have been without any of this.
 * <p>
 * Instances are immutable, so one can be reused for any number of compilations, including concurrent ones.
 * Build one with {@link #newBuilder()}.
 *
 * <h2>Why these numbers matter beyond compilation speed</h2>
 * <p>
 * A folded expression is not evaluated any more, and {@link RuntimeOptions}' budgets meter evaluation. So
 * these settings, not those, are what bounds the work a constant expression may do: {@code [range(0; 101)]}
 * builds its array while compiling, within {@link #getRuntimeOptions()}, and
 * {@link RuntimeOptions.Builder#setMaxArrayLength(int)} never sees it. The defaults are deliberately small
 * for that reason -- a few hundred values, elements or members -- because abandoning a fold is what puts an
 * expression back under the caller's own limits. {@link Builder#setEnabled(boolean) setEnabled(false)} puts
 * every expression back there.
 *
 * @see OptimizationOptions
 * @see RuntimeOptions
 */
public final class ConstantFoldingOptions {
	/**
	 * Bounded, unlike {@link RuntimeOptions}' own default. See
	 * {@link Builder#setRuntimeOptions(RuntimeOptions)}.
	 */
	private static final RuntimeOptions DEFAULT_RUNTIME_OPTIONS = RuntimeOptions.newBuilder()
			.setMaxArrayLength(256)
			.setMaxObjectMemberCount(256)
			.setMaxStringLength(4096)
			.setMaxOutputsPerExpression(256)
			.setMaxUserDefinedFunctionCalls(256)
			.build();

	private static final int DEFAULT_MAX_RESULTS = 256;

	private static final ConstantFoldingOptions DEFAULT = new ConstantFoldingOptions(true, DEFAULT_RUNTIME_OPTIONS, DEFAULT_MAX_RESULTS);

	private final boolean enabled;
	private final RuntimeOptions runtimeOptions;
	private final int maxResults;

	private ConstantFoldingOptions(boolean enabled, RuntimeOptions runtimeOptions, int maxResults) {
		this.enabled = enabled;
		this.runtimeOptions = runtimeOptions;
		this.maxResults = maxResults;
	}

	// Package-private: CompileOptions needs an instance for its own default, but callers never do -- they
	// either leave the setting alone or build one.
	static ConstantFoldingOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default: folding on, at most
	 * {@link #getMaxResults()} values per expression, evaluated under {@link #getRuntimeOptions()}.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Whether the compiler evaluates constant expressions at all.
	 *
	 * @return {@code true} if constant folding is on
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns the budgets a compile-time evaluation itself runs under -- how large a value it may build and
	 * how many values it may stream, exactly as they would bound an evaluation at runtime.
	 *
	 * @return the options, never {@code null}
	 */
	public RuntimeOptions getRuntimeOptions() {
		return runtimeOptions;
	}

	/**
	 * Returns the largest number of values one folded expression may produce.
	 *
	 * @return the maximum number of folded values per expression
	 */
	public int getMaxResults() {
		return maxResults;
	}

	/**
	 * Builds a {@link ConstantFoldingOptions}.
	 */
	public static final class Builder {
		private boolean enabled = true;
		private RuntimeOptions runtimeOptions = DEFAULT_RUNTIME_OPTIONS;
		private int maxResults = DEFAULT_MAX_RESULTS;

		private Builder() {
		}

		/**
		 * Sets whether the compiler evaluates constant expressions at all. On by default.
		 * <p>
		 * Turning it off is not only a matter of speed. Nothing is folded, so every expression is evaluated
		 * when the query is applied, which is where {@link RuntimeOptions}' budgets meter -- a caller who
		 * needs those budgets to cover a constant expression too wants this. It also costs the compile-time
		 * specialization that reads folded values: a regex module can no longer precompile a pattern built
		 * by an expression, such as {@code test("a" + "b")}, though a literal {@code test("ab")} is
		 * unaffected.
		 *
		 * @param enabled whether to fold constant expressions
		 * @return this, for chaining
		 */
		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/**
		 * Sets the budgets a compile-time evaluation runs under.
		 * <p>
		 * <strong>The default here is not {@link RuntimeOptions}' own default.</strong> That one bounds
		 * nothing, and these budgets are most of what bounds a fold: {@link #getMaxResults()} counts only
		 * the values that reach the compiler, so it does not bound an expression that works for a long
		 * time before emitting, or never emits at all. Pass
		 * {@code RuntimeOptions.newBuilder().build()} and {@code compile("last(range(0; 1e18))")} will not
		 * finish, where {@code compile("until(false; 1)")} would not finish at any budget that lets its
		 * loop run. What is bounded by default is {@link #getRuntimeOptions()} -- a few hundred elements,
		 * members and streamed values, and a few thousand characters -- which is what abandons such a fold
		 * promptly and leaves the expression to fail, or hang, where the caller can see it.
		 * <p>
		 * Passing the same instance here and to {@code JsonQuery#withRuntimeOptions} is how to say "fold
		 * under exactly the limits this query will run under", which the compiler cannot infer on its own:
		 * runtime options attach to an already-compiled query, strictly after every fold.
		 *
		 * @param runtimeOptions the budgets to evaluate under
		 * @return this, for chaining
		 * @throws NullPointerException if {@code runtimeOptions} is {@code null}
		 */
		public Builder setRuntimeOptions(RuntimeOptions runtimeOptions) {
			this.runtimeOptions = Objects.requireNonNull(runtimeOptions, "runtimeOptions");
			return this;
		}

		/**
		 * Sets the largest number of values one folded expression may produce.
		 * <p>
		 * An expression that produces more is not folded, so this is a statement about how many values are
		 * worth keeping for the life of a compiled query rather than a limit a query can fail. Zero is
		 * allowed and folds only an expression that produces nothing; to fold nothing at all, use
		 * {@link #setEnabled(boolean)}.
		 *
		 * @param maxResults the maximum number of folded values per expression
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxResults} is negative
		 */
		public Builder setMaxResults(int maxResults) {
			if (maxResults < 0)
				throw new IllegalArgumentException("maxResults must not be negative");
			this.maxResults = maxResults;
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public ConstantFoldingOptions build() {
			if (enabled && runtimeOptions == DEFAULT_RUNTIME_OPTIONS && maxResults == DEFAULT_MAX_RESULTS)
				return DEFAULT;
			return new ConstantFoldingOptions(enabled, runtimeOptions, maxResults);
		}
	}
}
