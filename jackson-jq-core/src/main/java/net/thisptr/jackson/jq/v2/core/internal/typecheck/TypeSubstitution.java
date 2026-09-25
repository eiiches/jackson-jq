package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Applies scheme-variable substitutions while respecting recursive-type binders.
 */
final class TypeSubstitution {
	private TypeSubstitution() {
	}

	/**
	 * Collects the type variables occurring free in {@code type} -- those not bound by an enclosing
	 * {@link RecursiveType}.
	 */
	static Set<TypeVariable> freeVariables(Type type) {
		Set<TypeVariable> free = new LinkedHashSet<>();
		collectFree(Objects.requireNonNull(type, "type"), new ArrayDeque<>(), free);
		return free;
	}

	private static void collectFree(Type type, Deque<TypeVariable> bound, Set<TypeVariable> free) {
		if (type instanceof TypeVariable variable) {
			if (!bound.contains(variable))
				free.add(variable);
		} else if (type instanceof ArrayType array) {
			for (Type knownElement : array.knownElements())
				collectFree(knownElement, bound, free);
			collectFree(array.additionalElementType(), bound, free);
		} else if (type instanceof ObjectType object) {
			for (Type field : object.fields().values())
				collectFree(field, bound, free);
			collectFree(object.additionalFieldType(), bound, free);
		} else if (type instanceof UnionType union) {
			for (Type alternative : union.alternatives())
				collectFree(alternative, bound, free);
		} else if (type instanceof RecursiveType recursive) {
			bound.push(recursive.variable());
			try {
				collectFree(recursive.body(), bound, free);
			} finally {
				bound.pop();
			}
		}
	}

	static Type apply(Type type, Set<TypeVariable> quantified, Map<TypeVariable, Type> substitutions) {
		return apply(type, quantified, substitutions, Map.of());
	}

	static Type apply(Type type, Set<TypeVariable> quantified, Map<TypeVariable, Type> substitutions,
					  Map<TypeVariable, Type> upperBounds) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(quantified, "quantified");
		Objects.requireNonNull(substitutions, "substitutions");
		Objects.requireNonNull(upperBounds, "upperBounds");
		return apply(type, quantified, substitutions, upperBounds, new ArrayDeque<>());
	}

	private static Type apply(Type type, Set<TypeVariable> quantified, Map<TypeVariable, Type> substitutions,
							  Map<TypeVariable, Type> upperBounds, Deque<TypeVariable> recursiveVariables) {
		if (type instanceof TypeVariable variable) {
			for (TypeVariable recursiveVariable : recursiveVariables) {
				if (recursiveVariable.equals(variable))
					return recursiveVariable;
			}
			if (!quantified.contains(variable))
				return variable;
			Type substitution = substitutions.get(variable);
			return substitution != null ? substitution
					: apply(upperBounds.getOrDefault(variable, AnyType.getInstance()), quantified, substitutions, upperBounds, recursiveVariables);
		}
		if (type instanceof ArrayType array) {
			List<Type> knownElements = new ArrayList<>(array.knownElements().size());
			for (Type knownElement : array.knownElements())
				knownElements.add(apply(knownElement, quantified, substitutions, upperBounds, recursiveVariables));
			return ArrayType.of(knownElements, apply(array.additionalElementType(), quantified, substitutions, upperBounds, recursiveVariables));
		}
		if (type instanceof ObjectType object) {
			Map<String, Type> fields = new LinkedHashMap<>();
			for (Map.Entry<String, Type> field : object.fields().entrySet()) {
				fields.put(field.getKey(), apply(field.getValue(), quantified, substitutions, upperBounds, recursiveVariables));
			}
			return ObjectType.of(fields, apply(object.additionalFieldType(), quantified, substitutions, upperBounds, recursiveVariables));
		}
		if (type instanceof UnionType union) {
			List<Type> alternatives = new ArrayList<>();
			for (Type alternative : union.alternatives())
				alternatives.add(apply(alternative, quantified, substitutions, upperBounds, recursiveVariables));
			return UnionType.of(alternatives);
		}
		if (type instanceof RecursiveType recursive) {
			TypeVariable variable = recursive.variable();
			recursiveVariables.push(variable);
			try {
				Type rewrittenBody = apply(recursive.body(), quantified, substitutions, upperBounds, recursiveVariables);
				if (rewrittenBody.equals(recursive.body()))
					return recursive;
				return RecursiveType.of(variable, rewrittenBody);
			} finally {
				recursiveVariables.pop();
			}
		}
		// All other permitted Type implementations are leaves.
		return type;
	}
}
