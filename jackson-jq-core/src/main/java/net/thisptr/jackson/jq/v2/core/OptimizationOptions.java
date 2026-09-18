package net.thisptr.jackson.jq.v2.core;

import java.util.Objects;

/**
 * Settings for compile-time optimizations, applied with
 * {@link CompileOptions.Builder#setOptimizationOptions(OptimizationOptions)}.
 * <p>
 * Instances are immutable, so one can be reused for any number of compilations, including concurrent
 * ones. Build one with {@link #newBuilder()}.
 *
 * @see CompileOptions
 * @see ConstantFoldingOptions
 */
public final class OptimizationOptions {
	private static final OptimizationOptions DEFAULT = new OptimizationOptions(ConstantFoldingOptions.getDefaultInstance(), true);

	private final ConstantFoldingOptions constantFoldingOptions;
	private final boolean tailCallOptimization;

	private OptimizationOptions(ConstantFoldingOptions constantFoldingOptions, boolean tailCallOptimization) {
		this.constantFoldingOptions = constantFoldingOptions;
		this.tailCallOptimization = tailCallOptimization;
	}

	// Package-private: CompileOptions needs an instance for its own default, but callers never do -- they
	// either leave the setting alone or build one.
	static OptimizationOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default: constant folding and tail-call optimization on.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns how much the compiler may evaluate while compiling.
	 *
	 * @return the constant-folding settings, never {@code null}
	 */
	public ConstantFoldingOptions getConstantFoldingOptions() {
		return constantFoldingOptions;
	}

	/**
	 * Returns whether a call in tail position is compiled as a loop.
	 *
	 * @return {@code true} if tail calls are optimized
	 */
	public boolean getTailCallOptimization() {
		return tailCallOptimization;
	}

	/**
	 * Builds an {@link OptimizationOptions}.
	 */
	public static final class Builder {
		private ConstantFoldingOptions constantFoldingOptions = ConstantFoldingOptions.getDefaultInstance();
		private boolean tailCallOptimization = true;

		private Builder() {
		}

		/**
		 * Sets how much the compiler may evaluate while compiling.
		 * <p>
		 * By default a constant expression is evaluated once, here, and the compiled query emits the values
		 * it found rather than running the expression again. See {@link ConstantFoldingOptions} for what
		 * that costs and for why turning it off changes which budgets bound a constant expression.
		 *
		 * @param constantFoldingOptions the constant-folding settings
		 * @return this, for chaining
		 * @throws NullPointerException if {@code constantFoldingOptions} is {@code null}
		 */
		public Builder setConstantFoldingOptions(ConstantFoldingOptions constantFoldingOptions) {
			this.constantFoldingOptions = Objects.requireNonNull(constantFoldingOptions, "constantFoldingOptions");
			return this;
		}

		/**
		 * Sets whether a call in tail position is compiled as a loop.
		 * <p>
		 * By default it is, which is what lets a recursive {@code def} -- and the jq-defined {@code until},
		 * {@code while} and {@code recurse} -- iterate as far as jq's own do instead of exhausting the Java
		 * stack after a few hundred iterations. Turning it off restores that old stack cost. It changes no
		 * values or ordering.
		 *
		 * @param tailCallOptimization whether to optimize tail calls
		 * @return this, for chaining
		 */
		public Builder setTailCallOptimization(boolean tailCallOptimization) {
			this.tailCallOptimization = tailCallOptimization;
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public OptimizationOptions build() {
			if (constantFoldingOptions == ConstantFoldingOptions.getDefaultInstance() && tailCallOptimization)
				return DEFAULT;
			return new OptimizationOptions(constantFoldingOptions, tailCallOptimization);
		}
	}
}
