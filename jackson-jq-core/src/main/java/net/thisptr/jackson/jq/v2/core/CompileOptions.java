package net.thisptr.jackson.jq.v2.core;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;

/**
 * Settings for a single call to {@link Environment#compile(String, CompileOptions)}.
 * <p>
 * Instances are immutable, so one can be reused for any number of compilations, including concurrent
 * ones. Build one with {@link #newBuilder()}.
 */
public final class CompileOptions {
	private static final CompileOptions DEFAULT = new CompileOptions(null, ConstantFoldingOptions.getDefaultInstance(), TailCallOptions.getDefaultInstance());

	private final @Nullable DiagnosticListener diagnosticListener;
	private final ConstantFoldingOptions constantFoldingOptions;
	private final TailCallOptions tailCallOptions;

	private CompileOptions(@Nullable DiagnosticListener diagnosticListener, ConstantFoldingOptions constantFoldingOptions, TailCallOptions tailCallOptions) {
		this.diagnosticListener = diagnosticListener;
		this.constantFoldingOptions = constantFoldingOptions;
		this.tailCallOptions = tailCallOptions;
	}

	// Package-private: Environment's no-options overload needs an instance to pass to compile(), but
	// callers never do -- they use the compile() overloads that take no options.
	static CompileOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default. No diagnostics are produced until a
	 * listener is set, and constant folding and tail-call optimization are on.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns who receives the diagnostics produced while compiling.
	 *
	 * @return the listener, or {@code null} if diagnostics are off
	 */
	public @Nullable DiagnosticListener getDiagnosticListener() {
		return diagnosticListener;
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
	 * @return the tail-call settings, never {@code null}
	 */
	public TailCallOptions getTailCallOptions() {
		return tailCallOptions;
	}

	/**
	 * Builds a {@link CompileOptions}.
	 */
	public static final class Builder {
		private @Nullable DiagnosticListener diagnosticListener;
		private ConstantFoldingOptions constantFoldingOptions = ConstantFoldingOptions.getDefaultInstance();
		private TailCallOptions tailCallOptions = TailCallOptions.getDefaultInstance();

		private Builder() {
		}

		/**
		 * Sets who receives the {@link Diagnostic}s produced while compiling.
		 * <p>
		 * With no listener the compiler skips diagnosis entirely, which is the default.
		 *
		 * @param diagnosticListener the listener, or {@code null} to produce no diagnostics
		 * @return this, for chaining
		 */
		public Builder setDiagnosticListener(@Nullable DiagnosticListener diagnosticListener) {
			this.diagnosticListener = diagnosticListener;
			return this;
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
		 * stack after a few hundred iterations. See {@link TailCallOptions} for exactly when it applies and
		 * what turning it off brings back.
		 *
		 * @param tailCallOptions the tail-call settings
		 * @return this, for chaining
		 * @throws NullPointerException if {@code tailCallOptions} is {@code null}
		 */
		public Builder setTailCallOptions(TailCallOptions tailCallOptions) {
			this.tailCallOptions = Objects.requireNonNull(tailCallOptions, "tailCallOptions");
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public CompileOptions build() {
			if (diagnosticListener == null && constantFoldingOptions == ConstantFoldingOptions.getDefaultInstance() && tailCallOptions == TailCallOptions.getDefaultInstance())
				return DEFAULT;
			return new CompileOptions(diagnosticListener, constantFoldingOptions, tailCallOptions);
		}
	}
}
