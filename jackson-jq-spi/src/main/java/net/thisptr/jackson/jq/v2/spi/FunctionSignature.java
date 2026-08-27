package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code (name, arity)} pair identifying a jq function.
 * <p>
 * A {@code null} {@link #arity()} means the function is variadic (bound under {@code name} alone),
 * matching the convention used by a negative {@code @FunctionRegistration#nargs()}.
 */
public class FunctionSignature {
	private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("@?[a-zA-Z_][a-zA-Z0-9_]*");

	private final String name;

	// null arity means that the function is variadic
	private final @Nullable Integer arity;

	private FunctionSignature(String name, @Nullable Integer arity) {
		validateName(name);
		validateArity(arity);
		this.name = name;
		this.arity = arity;
	}

	private static void validateName(String name) {
		Objects.requireNonNull(name, "name");
		if (!FUNCTION_NAME_PATTERN.matcher(name).matches())
			throw new IllegalArgumentException("Invalid function name: " + name);
	}

	private static void validateArity(@Nullable Integer arity) {
		if (arity != null && arity < 0)
			throw new IllegalArgumentException("Invalid arity (must be non-negative): " + arity);
	}

	/**
	 * Creates a signature for the given name and arity.
	 *
	 * @param name the function name
	 * @param arity the number of arguments, or {@code null} if variadic
	 * @return the signature
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name, or
	 *                                   {@code arity} is negative
	 */
	public static FunctionSignature of(String name, @Nullable Integer arity) {
		return new FunctionSignature(name, arity);
	}

	/**
	 * Returns the function name.
	 *
	 * @return the function name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the number of arguments, or {@code null} if this signature is variadic.
	 *
	 * @return the arity, or {@code null} if variadic
	 */
	public @Nullable Integer arity() {
		return arity;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof FunctionSignature))
			return false;
		FunctionSignature that = (FunctionSignature) o;
		return Objects.equals(name, that.name) && Objects.equals(arity, that.arity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, arity);
	}

	@Override
	public String toString() {
		return name + (arity != null ? "/" + arity : "");
	}

	/**
	 * Returns a copy of this signature with a different arity.
	 *
	 * @param arity the new arity, or {@code null} if variadic
	 * @return the new signature
	 * @throws IllegalArgumentException if {@code arity} is negative
	 */
	public FunctionSignature withArity(@Nullable Integer arity) {
		return new FunctionSignature(name, arity);
	}
}
