package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * A single formal parameter of a {@link JqFunction}: either a filter parameter, bound to the
 * caller's raw, unevaluated argument expression (e.g. {@code f} in {@code def map(f): ...}),
 * or a value parameter, bound to the argument's evaluated value and written with a leading
 * {@code $} in jq source (e.g. {@code $n} in {@code def limit($n; exp): ...}).
 * <p>
 * Instances are immutable value objects.
 */
public final class FunctionParameter {
	private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

	/**
	 * Distinguishes a filter parameter from a value ({@code $}-prefixed) parameter.
	 */
	public enum Kind {
		/**
		 * A filter parameter, bound to the caller's raw, unevaluated argument expression
		 * (e.g. {@code f} in {@code def map(f): ...}).
		 */
		FILTER,
		/**
		 * A value parameter, bound to the argument's evaluated value and written with a leading
		 * {@code $} in jq source (e.g. {@code $n} in {@code def limit($n; exp): ...}).
		 */
		VALUE
	}

	private final String name;
	private final Kind kind;

	private FunctionParameter(String name, Kind kind) {
		this.name = name;
		this.kind = kind;
	}

	/**
	 * Creates a filter parameter, e.g. {@code f} in {@code def map(f): ...}.
	 *
	 * @param name the parameter name, without a {@code $} prefix
	 * @return the filter parameter
	 * @throws IllegalArgumentException if {@code name} is not a valid parameter name
	 */
	public static FunctionParameter ofFilter(String name) {
		return new FunctionParameter(validateName(name), Kind.FILTER);
	}

	/**
	 * Creates a value parameter, e.g. {@code $n} in {@code def limit($n; exp): ...}.
	 *
	 * @param name the parameter name, without the {@code $} prefix
	 * @return the value parameter
	 * @throws IllegalArgumentException if {@code name} is not a valid parameter name
	 */
	public static FunctionParameter ofValue(String name) {
		return new FunctionParameter(validateName(name), Kind.VALUE);
	}

	/**
	 * Parses a single jq-syntax parameter: a name prefixed with {@code $} yields a
	 * {@link #ofValue}, any other name yields a {@link #ofFilter}.
	 *
	 * @param raw the jq-syntax parameter, e.g. {@code "f"} or {@code "$n"}
	 * @return the parsed parameter
	 * @throws IllegalArgumentException if the parameter name is invalid
	 */
	public static FunctionParameter valueOf(String raw) {
		Objects.requireNonNull(raw, "raw");
		return raw.startsWith("$") ? ofValue(raw.substring(1)) : ofFilter(raw);
	}

	private static String validateName(String name) {
		Objects.requireNonNull(name, "name");
		if (!PARAMETER_NAME_PATTERN.matcher(name).matches())
			throw new IllegalArgumentException("Invalid parameter name: " + name);
		return name;
	}

	/**
	 * Returns the parameter name, without a {@code $} prefix even for a {@link Kind#VALUE}
	 * parameter.
	 *
	 * @return the parameter name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns whether this is a filter or a value parameter.
	 *
	 * @return the parameter kind
	 */
	public Kind kind() {
		return kind;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof FunctionParameter))
			return false;
		FunctionParameter that = (FunctionParameter) o;
		return kind == that.kind && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, kind);
	}

	@Override
	public String toString() {
		return kind == Kind.VALUE ? "$" + name : name;
	}
}
