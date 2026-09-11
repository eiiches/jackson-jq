package net.thisptr.jackson.jq.v2.core.diagnostic;

/**
 * Receives the {@link Diagnostic}s the compiler produces for one query.
 * <p>
 * Every diagnostic for a compilation is reported before that {@code compile()} call returns, on
 * the calling thread. Implementations must not throw; a listener that does will abort the
 * compilation.
 */
public interface DiagnosticListener {

	/**
	 * Called once for each diagnostic, in source order.
	 *
	 * @param diagnostic the diagnostic
	 */
	void report(Diagnostic diagnostic);
}
