package net.thisptr.jackson.jq.v2.core.diagnostic;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A single message the compiler produced about the query it was given.
 * <p>
 * A diagnostic never stops compilation -- anything that does is thrown as a
 * {@code JsonQueryException} instead.
 */
public final class Diagnostic {

	/**
	 * How much the reader should care about a {@link Diagnostic}.
	 */
	public enum Severity {
		/**
		 * The query compiles and runs, but probably does not mean what its author intended.
		 */
		WARNING,

		/**
		 * The query is wrong, but the compiler was able to carry on past it.
		 */
		ERROR
	}

	private final Severity severity;
	private final String message;
	private final @Nullable SourceLocation location;

	private Diagnostic(Severity severity, String message, @Nullable SourceLocation location) {
		this.severity = Objects.requireNonNull(severity, "severity");
		this.message = Objects.requireNonNull(message, "message");
		this.location = location;
	}

	/**
	 * Creates a diagnostic.
	 *
	 * @param severity how much the reader should care
	 * @param message the human-readable message
	 * @param location where in the query this is about, or {@code null} if not known
	 * @return the diagnostic
	 */
	public static Diagnostic of(Severity severity, String message, @Nullable SourceLocation location) {
		return new Diagnostic(severity, message, location);
	}

	/**
	 * Creates a {@link Severity#WARNING} diagnostic.
	 *
	 * @param message the human-readable message
	 * @param location where in the query this is about, or {@code null} if not known
	 * @return the diagnostic
	 */
	public static Diagnostic warning(String message, @Nullable SourceLocation location) {
		return new Diagnostic(Severity.WARNING, message, location);
	}

	/**
	 * Returns how much the reader should care about this diagnostic.
	 *
	 * @return the severity
	 */
	public Severity severity() {
		return severity;
	}

	/**
	 * Returns the human-readable message.
	 *
	 * @return the message
	 */
	public String message() {
		return message;
	}

	/**
	 * Returns where in the query this diagnostic is about.
	 *
	 * @return the location, or {@code null} if not known
	 */
	public @Nullable SourceLocation location() {
		return location;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof Diagnostic))
			return false;
		Diagnostic that = (Diagnostic) o;
		return severity == that.severity && message.equals(that.message) && Objects.equals(location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(severity, message, location);
	}

	@Override
	public String toString() {
		String prefix = severity.name().toLowerCase(Locale.ROOT) + ": " + message;
		return location != null ? prefix + " at " + location : prefix;
	}
}
