package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

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

	public static FunctionSignature of(String name, int arity) {
		return new FunctionSignature(name, arity);
	}

	public String name() {
		return name;
	}

	public @Nullable Integer arity() {
		return arity;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
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

	public FunctionSignature withArity(@Nullable Integer arity) {
		return new FunctionSignature(name, arity);
	}
}
