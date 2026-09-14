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
public final class FunctionSignature {
	private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("@?[a-zA-Z_][a-zA-Z0-9_]*");
	private static final Pattern ARITY_PATTERN = Pattern.compile("0|[1-9][0-9]*");

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
	 * Creates a signature for the given name with no arity constraint (variadic).
	 *
	 * @param name the function name
	 * @return the signature
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name
	 */
	public static FunctionSignature ofVariadic(String name) {
		return new FunctionSignature(name, null);
	}

	/**
	 * Creates a signature for the given name and arity.
	 *
	 * @param name the function name
	 * @param arity the number of arguments (must be non-negative)
	 * @return the signature
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name, or
	 * {@code arity} is negative
	 */
	public static FunctionSignature of(String name, int arity) {
		return new FunctionSignature(name, arity);
	}

	/**
	 * Parses a function signature from its {@code name/arity} or {@code name/*} string form
	 * (e.g. {@code "length/0"} or {@code "custom/*"}).
	 *
	 * @param text the string form to parse
	 * @return the parsed function signature
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 */
	public static FunctionSignature valueOf(String text) {
		Objects.requireNonNull(text, "text");
		int slash = text.indexOf('/');
		if (slash < 0)
			throw new IllegalArgumentException("Invalid function signature (expected name/arity or name/*): " + text);
		String name = text.substring(0, slash);
		String arityStr = text.substring(slash + 1);
		if ("*".equals(arityStr))
			return ofVariadic(name);
		if (!ARITY_PATTERN.matcher(arityStr).matches())
			throw new IllegalArgumentException("Invalid arity in function signature: " + text);
		int arity;
		try {
			arity = Integer.parseInt(arityStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid arity in function signature: " + text, e);
		}
		return of(name, arity);
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

	/**
	 * Returns {@code true} if this signature is variadic (has no arity constraint).
	 *
	 * @return whether this signature is variadic
	 */
	public boolean isVariadic() {
		return arity == null;
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
		return name + "/" + (arity != null ? arity : "*");
	}

	/**
	 * Returns a copy of this signature with a different arity.
	 *
	 * @param arity the new arity (must be non-negative)
	 * @return the new signature
	 * @throws IllegalArgumentException if {@code arity} is negative
	 */
	public FunctionSignature withArity(int arity) {
		return new FunctionSignature(name, arity);
	}

	/**
	 * Returns a variadic copy of this signature (with no arity constraint).
	 *
	 * @return the variadic signature
	 */
	public FunctionSignature asVariadic() {
		return arity == null ? this : ofVariadic(name);
	}
}
