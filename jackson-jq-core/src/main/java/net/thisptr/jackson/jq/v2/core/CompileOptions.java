package net.thisptr.jackson.jq.v2.core;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.Type;

/**
 * Settings for a single call to {@link Environment#compile(String, CompileOptions)}.
 * <p>
 * Instances are immutable, so one can be reused for any number of compilations, including concurrent
 * ones. Build one with {@link #newBuilder()}.
 */
public final class CompileOptions {
	private static final CompileOptions DEFAULT = new CompileOptions(null, OptimizationOptions.getDefaultInstance(), TypeCheckMode.OFF, AnyType.getInstance(), AnyType.getInstance());

	private final @Nullable DiagnosticListener diagnosticListener;
	private final OptimizationOptions optimizationOptions;
	private final TypeCheckMode typeCheckMode;
	private final Type inputType;
	private final Type outputType;

	private CompileOptions(@Nullable DiagnosticListener diagnosticListener, OptimizationOptions optimizationOptions, TypeCheckMode typeCheckMode, Type inputType, Type outputType) {
		this.diagnosticListener = diagnosticListener;
		this.optimizationOptions = optimizationOptions;
		this.typeCheckMode = typeCheckMode;
		this.inputType = inputType;
		this.outputType = outputType;
	}

	// Package-private: Environment's no-options overload needs an instance to pass to compile(), but
	// callers never do -- they use the compile() overloads that take no options.
	static CompileOptions getDefaultInstance() {
		return DEFAULT;
	}

	/**
	 * Creates a builder with every setting at its default. Type checking is off, no diagnostics are
	 * produced until a listener is set, and every optimization is on.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Creates a builder holding every setting of this instance, for deriving options that differ in
	 * only some of them.
	 *
	 * @return a new builder, never {@code null}
	 */
	public Builder toBuilder() {
		return new Builder()
				.setDiagnosticListener(diagnosticListener)
				.setOptimizationOptions(optimizationOptions)
				.setTypeCheckMode(typeCheckMode)
				.setInputType(inputType)
				.setOutputType(outputType);
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

	public TypeCheckMode getTypeCheckMode() {
		return typeCheckMode;
	}

	/**
	 * Returns what the query's {@code .} input is taken to be.
	 *
	 * @return the input type, never {@code null}
	 */
	public Type getInputType() {
		return inputType;
	}

	/**
	 * Returns what the query's results must be assignable to.
	 *
	 * @return the output type, never {@code null}
	 */
	public Type getOutputType() {
		return outputType;
	}

	/**
	 * Builds a {@link CompileOptions}.
	 */
	public static final class Builder {
		private @Nullable DiagnosticListener diagnosticListener;
		private OptimizationOptions optimizationOptions = OptimizationOptions.getDefaultInstance();
		private TypeCheckMode typeCheckMode = TypeCheckMode.OFF;
		private Type inputType = AnyType.getInstance();
		private Type outputType = AnyType.getInstance();

		private Builder() {
		}

		/**
		 * Sets who receives the {@link Diagnostic}s produced while compiling.
		 * <p>
		 * With no listener diagnostics are discarded. Passes explicitly enabled by another option still
		 * run, for example to infer the compiled query's type.
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

		public Builder setTypeCheckMode(TypeCheckMode typeCheckMode) {
			this.typeCheckMode = Objects.requireNonNull(typeCheckMode, "typeCheckMode");
			return this;
		}

		/**
		 * Sets what the query's {@code .} input is. Type checking starts from this type, so the more it
		 * says, the more the checker can tell. The default, {@link AnyType}, says nothing and every query
		 * checks.
		 *
		 * @param inputType the input type
		 * @return this, for chaining
		 * @throws NullPointerException if {@code inputType} is {@code null}
		 */
		public Builder setInputType(Type inputType) {
			this.inputType = Objects.requireNonNull(inputType, "inputType");
			return this;
		}

		/**
		 * Sets what the query's results must be assignable to. Unlike the input type, this is checked
		 * rather than inferred from: the compiled query still reports the output type that was actually
		 * inferred, and a query whose results the declaration does not accept is diagnosed like any other
		 * type error -- a warning under {@link TypeCheckMode#WARN}, an error under
		 * {@link TypeCheckMode#STRICT}. Under {@link TypeCheckMode#OFF} nothing is inferred, so there is
		 * nothing to check and this setting has no effect.
		 * <p>
		 * The default, {@link AnyType}, accepts anything. So does an inferred output of {@link AnyType},
		 * which is what a query the checker cannot pin down produces, so a declaration catches results
		 * known not to fit rather than results not known to fit.
		 *
		 * @param outputType the output type
		 * @return this, for chaining
		 * @throws NullPointerException if {@code outputType} is {@code null}
		 */
		public Builder setOutputType(Type outputType) {
			this.outputType = Objects.requireNonNull(outputType, "outputType");
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public CompileOptions build() {
			if (diagnosticListener == null && optimizationOptions == OptimizationOptions.getDefaultInstance()
					&& typeCheckMode == TypeCheckMode.OFF && inputType == AnyType.getInstance() && outputType == AnyType.getInstance())
				return DEFAULT;
			return new CompileOptions(diagnosticListener, optimizationOptions, typeCheckMode, inputType, outputType);
		}
	}
}
