package net.thisptr.jackson.jq.v2.core;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;

/**
 * Settings for a single call to {@link Environment#compile(String, CompileOptions)}.
 * <p>
 * Options are read when {@code compile()} is called and copied into the query it returns, so
 * changing a {@code CompileOptions} afterwards never affects a query already compiled from it, and
 * one instance can be reused for any number of compilations.
 */
public final class CompileOptions {
	private @Nullable DiagnosticListener diagnosticListener;

	/**
	 * Creates options with every setting at its default. No diagnostics are produced until a
	 * listener is set.
	 */
	public CompileOptions() {
	}

	private CompileOptions(CompileOptions other) {
		this.diagnosticListener = other.diagnosticListener;
	}

	/**
	 * Sets who receives the {@link Diagnostic}s produced while compiling.
	 * <p>
	 * With no listener the compiler skips diagnosis entirely, which is the default.
	 *
	 * @param diagnosticListener the listener, or {@code null} to produce no diagnostics
	 * @return this, for chaining
	 */
	public CompileOptions setDiagnosticListener(@Nullable DiagnosticListener diagnosticListener) {
		this.diagnosticListener = diagnosticListener;
		return this;
	}

	/**
	 * Returns who receives the diagnostics produced while compiling.
	 *
	 * @return the listener, or {@code null} if diagnostics are off
	 */
	public @Nullable DiagnosticListener getDiagnosticListener() {
		return diagnosticListener;
	}

	CompileOptions copy() {
		return new CompileOptions(this);
	}
}
