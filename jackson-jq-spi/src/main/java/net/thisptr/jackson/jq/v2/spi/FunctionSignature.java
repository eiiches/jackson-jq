package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class FunctionSignature {
	private final String name;

	// null arity means that the function is variadic
	private final @Nullable Integer arity;

	private FunctionSignature(String name, @Nullable Integer arity) {
		this.name = name;
		this.arity = arity;
	}

	public static FunctionSignature of(String name, int arity) {
		// TODO: validate name here
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
