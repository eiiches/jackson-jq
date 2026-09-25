package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A type variable quantified by a {@link TypeScheme} or bound by a {@link RecursiveType}.
 * <p>
 * Variables are values: two variables are the same variable when their names are equal, so the
 * same variable may be written as {@code TypeVariable.of("Element")} wherever it occurs. Distinct
 * variables therefore need distinct names.
 */
public final class TypeVariable implements Type {
	private final String name;

	/**
	 * Returns a type variable with the given name.
	 *
	 * @param name the name of the variable
	 * @return the type variable
	 * @throws NullPointerException if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} is blank or a reserved type name
	 */
	public static TypeVariable of(String name) {
		return new TypeVariable(name);
	}

	private TypeVariable(String name) {
		this.name = Objects.requireNonNull(name, "name");
		if (name.isBlank())
			throw new IllegalArgumentException("A type variable name must not be blank");
		// A variable named after a keyword would be read back as that keyword, so it is rejected. A
		// name that is not an identifier is allowed: it simply has no spelling the parser accepts,
		// which is what lets a generated name be one no written signature can collide with.
		if (TypeNotation.isReservedName(name))
			throw new IllegalArgumentException("A type variable name must not be a reserved type name: " + name);
	}

	public String name() {
		return name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof TypeVariable other && name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
