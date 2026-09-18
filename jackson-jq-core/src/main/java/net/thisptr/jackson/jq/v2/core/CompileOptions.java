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
	private static final CompileOptions DEFAULT = new CompileOptions(null, OptimizationOptions.getDefaultInstance());

	private final @Nullable DiagnosticListener diagnosticListener;
	private final OptimizationOptions optimizationOptions;

	private CompileOptions(@Nullable DiagnosticListener diagnosticListener, OptimizationOptions optimizationOptions) {
		this.diagnosticListener = diagnosticListener;
		this.optimizationOptions = optimizationOptions;
	}

	// Package-private: Environment's no-options overload needs an instance to pass to compile(), but
	// callers never do -- they use the compile() overloads that take no options.
	static CompileOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default. No diagnostics are produced until a
	 * listener is set, and every optimization is on.
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
	 * Returns the compile-time optimization settings.
	 *
	 * @return the optimization settings, never {@code null}
	 */
	public OptimizationOptions getOptimizationOptions() {
		return optimizationOptions;
	}

	/**
	 * Builds a {@link CompileOptions}.
	 */
	public static final class Builder {
		private @Nullable DiagnosticListener diagnosticListener;
		private OptimizationOptions optimizationOptions = OptimizationOptions.getDefaultInstance();

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
		 * Sets the compile-time optimization settings.
		 *
		 * @param optimizationOptions the optimization settings
		 * @return this, for chaining
		 * @throws NullPointerException if {@code optimizationOptions} is {@code null}
		 */
		public Builder setOptimizationOptions(OptimizationOptions optimizationOptions) {
			this.optimizationOptions = Objects.requireNonNull(optimizationOptions, "optimizationOptions");
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public CompileOptions build() {
			if (diagnosticListener == null && optimizationOptions == OptimizationOptions.getDefaultInstance())
				return DEFAULT;
			return new CompileOptions(diagnosticListener, optimizationOptions);
		}
	}
}
