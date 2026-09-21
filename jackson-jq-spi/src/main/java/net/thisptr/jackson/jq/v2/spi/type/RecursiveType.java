package net.thisptr.jackson.jq.v2.spi.type;

import java.util.IdentityHashMap;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * An equi-recursive type written {@code RECURSIVE<variable = body>}.
 * <p>
 * The variable names the recursive type itself.
 *
 * <p>{@link #equals(Object)} is structural: two recursive types are equal when their bound
 * {@link TypeVariable}s and their bodies are equal, so types differing only in the name of the bound
 * variable are not equal.
 */
public final class RecursiveType implements Type {
	private final TypeVariable variable;
	private final Type body;

	public static RecursiveType of(TypeVariable variable, Type body) {
		return new RecursiveType(variable, body);
	}

	private RecursiveType(TypeVariable variable, Type body) {
		this.variable = Objects.requireNonNull(variable, "variable");
		this.body = Objects.requireNonNull(body, "body");
		if (!occurs(body, variable, new IdentityHashMap<>()))
			throw new IllegalArgumentException("A recursive type body must reference its bound variable");
		if (!guarded(body, variable, false, new IdentityHashMap<>()))
			throw new IllegalArgumentException("Recursive type references must occur below an array or object");
	}

	public TypeVariable variable() {
		return variable;
	}

	public Type body() {
		return body;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof RecursiveType other && variable.equals(other.variable) && body.equals(other.body);
	}

	@Override
	public int hashCode() {
		return Objects.hash(variable, body);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}

	// The visited maps below memoize sub-objects the body shares, so that a deeply shared subtree is
	// walked once. They are keyed by identity because they say nothing about what a type means.

	private static boolean occurs(Type type, TypeVariable variable, IdentityHashMap<Type, Boolean> visited) {
		if (variable.equals(type))
			return true;
		if (visited.put(type, Boolean.TRUE) != null)
			return false;
		if (type instanceof ArrayType array) {
			if (occurs(array.additionalElementType(), variable, visited))
				return true;
			for (Type knownElement : array.knownElements()) {
				if (occurs(knownElement, variable, visited))
					return true;
			}
		}
		if (type instanceof ObjectType object) {
			if (occurs(object.additionalFieldType(), variable, visited))
				return true;
			for (Type field : object.fields().values()) {
				if (occurs(field, variable, visited))
					return true;
			}
		}
		if (type instanceof UnionType union) {
			for (Type alternative : union.alternatives()) {
				if (occurs(alternative, variable, visited))
					return true;
			}
		}
		if (type instanceof RecursiveType recursive && !recursive.variable.equals(variable))
			return occurs(recursive.body, variable, visited);
		return false;
	}

	private static boolean guarded(Type type, TypeVariable variable, boolean beneathContainer,
								   IdentityHashMap<Type, Boolean> visited) {
		if (variable.equals(type))
			return beneathContainer;
		if (visited.put(type, Boolean.TRUE) != null)
			return true;
		if (type instanceof ArrayType array) {
			if (!guarded(array.additionalElementType(), variable, true, visited))
				return false;
			for (Type knownElement : array.knownElements()) {
				if (!guarded(knownElement, variable, true, visited))
					return false;
			}
		}
		if (type instanceof ObjectType object) {
			if (!guarded(object.additionalFieldType(), variable, true, visited))
				return false;
			for (Type field : object.fields().values()) {
				if (!guarded(field, variable, true, visited))
					return false;
			}
		}
		if (type instanceof UnionType union) {
			for (Type alternative : union.alternatives()) {
				if (!guarded(alternative, variable, beneathContainer, visited))
					return false;
			}
		}
		if (type instanceof RecursiveType recursive && !recursive.variable.equals(variable))
			return guarded(recursive.body, variable, beneathContainer, visited);
		return true;
	}
}
