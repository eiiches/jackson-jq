package net.thisptr.jackson.jq.v2.spi.type;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * An immutable, universally quantified jq filter or function signature.
 * <p>
 * A scheme pairs a {@link FilterType} or {@link FunctionType} body with the {@link TypeVariable}s it
 * binds. A scheme without type variables is monomorphic. A scheme with type variables is polymorphic:
 * its variables are instantiated afresh each time the scheme is applied, allowing the relationships
 * expressed by the body to be reused with different concrete types.
 * <p>
 * Every type variable occurring free in the body or in a quantified variable's upper bound must be
 * listed in {@link #typeVariables()}. Variables bound by a {@link RecursiveType} are not free, and
 * quantified variables may be unused. Duplicate variable names and unquantified free variables are
 * rejected when the scheme is created.
 * <p>
 * Its string representation prefixes a quantified type with {@code <T, U>}, and writes a
 * variable's upper bound after a colon, as {@code <T: STRING>}.
 *
 * @param <T> the signature type, either {@link FilterType} or {@link FunctionType}
 */
public final class TypeScheme<T> {
	private final Map<TypeVariable, Type> variables;
	private final T type;

	private TypeScheme(Map<? extends TypeVariable, ? extends Type> variables, T type) {
		this.type = Objects.requireNonNull(type, "type");
		Objects.requireNonNull(variables, "variables");
		Map<TypeVariable, Type> bounds = new LinkedHashMap<>();
		for (Map.Entry<? extends TypeVariable, ? extends Type> entry : variables.entrySet()) {
			TypeVariable variable = Objects.requireNonNull(entry.getKey(), "type variable");
			Type bound = Objects.requireNonNullElse(entry.getValue(), AnyType.getInstance());
			bounds.put(variable, bound);
		}
		this.variables = Collections.unmodifiableMap(bounds);
		validateVariables();
	}

	/**
	 * Creates a monomorphic filter type scheme.
	 *
	 * @param type the filter signature
	 * @return a scheme with no quantified type variables
	 */
	public static TypeScheme<FilterType> of(FilterType type) {
		return new TypeScheme<>(Map.of(), type);
	}

	/**
	 * Creates a filter type scheme that universally quantifies the given variables with their upper bounds.
	 *
	 * @param variables the type variables bound by the scheme mapped to their upper bounds
	 * @param type the filter signature in which the variables may occur
	 * @return the quantified filter type scheme
	 * @throws IllegalArgumentException if a free variable is not quantified
	 */
	public static TypeScheme<FilterType> of(Map<? extends TypeVariable, ? extends Type> variables, FilterType type) {
		return new TypeScheme<>(variables, type);
	}

	/**
	 * Creates a monomorphic function type scheme.
	 *
	 * @param type the function signature
	 * @return a scheme with no quantified type variables
	 */
	public static TypeScheme<FunctionType> of(FunctionType type) {
		return new TypeScheme<>(Map.of(), type);
	}

	/**
	 * Creates a function type scheme that universally quantifies the given variables with their upper bounds.
	 *
	 * @param variables the type variables bound by the scheme mapped to their upper bounds
	 * @param type the function signature in which the variables may occur
	 * @return the quantified function type scheme
	 * @throws IllegalArgumentException if a free variable is not quantified
	 */
	public static TypeScheme<FunctionType> of(Map<? extends TypeVariable, ? extends Type> variables,
											  FunctionType type) {
		return new TypeScheme<>(variables, type);
	}

	/**
	 * Parses a filter type scheme written in the notation of this package, with an optional
	 * {@code <T, U>} quantifier before the {@code input -> output} body.
	 * <p>
	 * A variable's upper bound is read from its binder; a name with no binder in scope parses as an
	 * unbounded variable, since only a binder can say otherwise.
	 *
	 * @param text the string form to parse
	 * @return the parsed filter type scheme
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public static TypeScheme<FilterType> ofFilter(String text) {
		return TypeNotation.parseFilterScheme(Objects.requireNonNull(text, "text"));
	}

	/**
	 * Parses a function type scheme written in the notation of this package, with an optional
	 * {@code <T, U>} quantifier before the {@code (parameters) => (input -> output)} body.
	 * <p>
	 * A variable's upper bound is read from its binder; a name with no binder in scope parses as an
	 * unbounded variable, since only a binder can say otherwise.
	 *
	 * @param text the string form to parse
	 * @return the parsed function type scheme
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public static TypeScheme<FunctionType> ofFunction(String text) {
		return TypeNotation.parseFunctionScheme(Objects.requireNonNull(text, "text"));
	}

	/**
	 * Returns the type variables universally quantified by this scheme mapped to their upper bounds,
	 * in declaration order. Any variable without an explicitly declared upper bound maps to {@link AnyType}.
	 *
	 * @return the unmodifiable map of quantified type variables to their upper bounds
	 */
	public Map<TypeVariable, Type> typeVariables() {
		return variables;
	}

	/**
	 * Returns the filter or function signature governed by this scheme.
	 *
	 * @return the signature body
	 */
	public T body() {
		return type;
	}

	private void validateVariables() {
		Set<TypeVariable> quantified = variables.keySet();
		Set<TypeVariable> free = new LinkedHashSet<>();
		Deque<TypeVariable> recursiveVariables = new ArrayDeque<>();
		for (Type bound : variables.values())
			collectFreeVariables(bound, recursiveVariables, free);
		if (type instanceof FilterType filter) {
			collectFreeVariables(filter.inputType(), recursiveVariables, free);
			collectFreeVariables(filter.outputType(), recursiveVariables, free);
		} else if (type instanceof FunctionType function) {
			collectFreeVariables(function.returnType().inputType(), recursiveVariables, free);
			collectFreeVariables(function.returnType().outputType(), recursiveVariables, free);
			for (FilterType parameter : function.parameterTypes()) {
				collectFreeVariables(parameter.inputType(), recursiveVariables, free);
				collectFreeVariables(parameter.outputType(), recursiveVariables, free);
			}
		} else {
			throw new IllegalArgumentException("Unsupported type scheme body: " + type.getClass().getName());
		}
		free.removeAll(quantified);
		if (!free.isEmpty())
			throw new IllegalArgumentException("Type scheme contains free type variables: " + free);
	}

	private static void collectFreeVariables(Type type, Deque<TypeVariable> recursiveVariables,
											 Set<TypeVariable> free) {
		if (type instanceof TypeVariable variable) {
			if (!recursiveVariables.contains(variable))
				free.add(variable);
		} else if (type instanceof ArrayType array) {
			for (Type knownElement : array.knownElements())
				collectFreeVariables(knownElement, recursiveVariables, free);
			collectFreeVariables(array.additionalElementType(), recursiveVariables, free);
		} else if (type instanceof ObjectType object) {
			for (Type field : object.fields().values())
				collectFreeVariables(field, recursiveVariables, free);
			collectFreeVariables(object.additionalFieldType(), recursiveVariables, free);
		} else if (type instanceof UnionType union) {
			for (Type alternative : union.alternatives())
				collectFreeVariables(alternative, recursiveVariables, free);
		} else if (type instanceof RecursiveType recursive) {
			recursiveVariables.push(recursive.variable());
			try {
				collectFreeVariables(recursive.body(), recursiveVariables, free);
			} finally {
				recursiveVariables.pop();
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof TypeScheme<?> other && variables.equals(other.variables)
				&& type.equals(other.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(variables, type);
	}

	@Override
	public String toString() {
		return TypeNotation.print(variables, type);
	}
}
