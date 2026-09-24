package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Matches actual types against scheme types and collects constraints for quantified variables.
 */
final class TypeMatcher {
	private final Set<TypeVariable> quantified;
	private final Map<TypeVariable, Type> upperBounds;
	private final boolean strictSubtyping;
	private final Map<TypeVariable, Type> substitutions = new HashMap<>();
	private final Deque<TypeVariable> expectedRecursiveVariables = new ArrayDeque<>();
	private final Deque<TypeVariable> actualRecursiveVariables = new ArrayDeque<>();

	TypeMatcher(TypeScheme<?> scheme) {
		this(scheme.typeVariables().keySet(), scheme.typeVariables());
	}

	TypeMatcher(Set<TypeVariable> quantified) {
		this(quantified, Map.of());
	}

	TypeMatcher(Set<TypeVariable> quantified, Map<TypeVariable, Type> upperBounds) {
		this(quantified, upperBounds, false);
	}

	TypeMatcher(Set<TypeVariable> quantified, Map<TypeVariable, Type> upperBounds, boolean strictSubtyping) {
		this.quantified = Set.copyOf(quantified);
		this.upperBounds = Map.copyOf(upperBounds);
		this.strictSubtyping = strictSubtyping;
	}

	/**
	 * Matches {@code actual} against {@code expected}, preserving prior constraints on failure.
	 */
	boolean match(Type expected, Type actual) {
		Objects.requireNonNull(expected, "expected");
		Objects.requireNonNull(actual, "actual");
		Map<TypeVariable, Type> before = new HashMap<>(substitutions);
		if (matchInternal(expected, actual))
			return true;
		restore(before);
		return false;
	}

	/**
	 * Returns whether all inferred types satisfy their fully substituted upper bounds.
	 */
	boolean validateBounds() {
		for (Map.Entry<TypeVariable, Type> substitution : substitutions.entrySet()) {
			Type bound = upperBounds.getOrDefault(substitution.getKey(), AnyType.getInstance());
			Type upperBound = substitute(bound);
			if (!accepts(upperBound, substitution.getValue()))
				return false;
		}
		return true;
	}

	Type substitute(Type type) {
		return TypeSubstitution.apply(type, quantified, substitutions, upperBounds);
	}

	static boolean accepts(Type expected, Type actual) {
		TypeMatcher matcher = new TypeMatcher(Set.of());
		return matcher.match(expected, actual);
	}

	static boolean isSubtype(Type subtype, Type supertype) {
		TypeMatcher matcher = new TypeMatcher(Set.of(), Map.of(), true);
		return matcher.match(supertype, subtype);
	}

	private boolean matchInternal(Type expected, Type actual) {
		if (actual == NeverType.getInstance()) {
			if (expected instanceof TypeVariable variable && quantified.contains(variable))
				return constrain(variable, actual);
			return true;
		}
		if (expected == NeverType.getInstance())
			return false;

		if (actual instanceof UnionType union) {
			for (Type alternative : union.alternatives()) {
				if (!matchInternal(expected, alternative))
					return false;
			}
			return true;
		}

		if (expected instanceof UnionType union)
			return matchExpectedUnion(union, actual);

		if (expected instanceof TypeVariable || actual instanceof TypeVariable) {
			int expectedBinder = binderIndex(expectedRecursiveVariables, expected);
			int actualBinder = binderIndex(actualRecursiveVariables, actual);
			if (expectedBinder >= 0 || actualBinder >= 0)
				return expectedBinder >= 0 && expectedBinder == actualBinder;
			if (expected instanceof TypeVariable variable && quantified.contains(variable))
				return constrain(variable, actual);
		}

		if (expected instanceof UndefinedType || actual instanceof UndefinedType)
			return expected instanceof UndefinedType && actual instanceof UndefinedType;
		if (strictSubtyping) {
			if (expected instanceof AnyType)
				return true;
			if (actual instanceof AnyType)
				return false;
		} else {
			if (expected instanceof AnyType || actual instanceof AnyType)
				return true;
		}

		if (expected instanceof ArrayType || actual instanceof ArrayType) {
			return expected instanceof ArrayType expectedArray && actual instanceof ArrayType actualArray
					&& matchArrays(expectedArray, actualArray);
		}

		if (expected instanceof ObjectType || actual instanceof ObjectType) {
			return expected instanceof ObjectType expectedObject && actual instanceof ObjectType actualObject
					&& matchObjects(expectedObject, actualObject);
		}

		if (expected instanceof RecursiveType || actual instanceof RecursiveType) {
			return expected instanceof RecursiveType expectedRecursive && actual instanceof RecursiveType actualRecursive
					&& matchRecursive(expectedRecursive, actualRecursive);
		}

		if (expected instanceof NumericType expectedNum && actual instanceof NumericType actualNum) {
			if (strictSubtyping)
				return expectedNum.numberKind() == NumberKind.UNKNOWN || expectedNum.numberKind() == actualNum.numberKind();
			return true;
		}

		if (expected instanceof StringType expectedString && actual instanceof StringType actualString)
			return matchesValue(expectedString.value(), actualString.value());

		if (expected instanceof BooleanType expectedBoolean && actual instanceof BooleanType actualBoolean)
			return matchesValue(expectedBoolean.value(), actualBoolean.value());

		return TypeEquivalence.isEqualType(expected, actual);
	}

	/**
	 * Whether a type describing a known value meets one describing another. An expectation naming no
	 * value is met by anything, and two named values must be the same one. An expectation that names a
	 * value where the actual type does not is a maybe: assignability lets it through, since the value
	 * may well be the one asked for, while subtyping does not, since it is not known to be.
	 */
	private boolean matchesValue(@Nullable Object expected, @Nullable Object actual) {
		if (expected == null)
			return true;
		if (actual == null)
			return !strictSubtyping;
		return expected.equals(actual);
	}

	private boolean matchExpectedUnion(UnionType expected, Type actual) {
		Map<TypeVariable, Type> before = new HashMap<>(substitutions);
		List<Map<TypeVariable, Type>> matches = new ArrayList<>();
		for (Type alternative : expected.alternatives()) {
			restore(before);
			if (matchInternal(alternative, actual))
				matches.add(new HashMap<>(substitutions));
		}
		restore(before);
		if (matches.isEmpty())
			return false;
		for (Map<TypeVariable, Type> match : matches)
			merge(match);
		return true;
	}

	private boolean constrain(TypeVariable variable, Type actual) {
		Map<TypeVariable, Type> before = new HashMap<>(substitutions);
		Type bound = upperBounds.getOrDefault(variable, AnyType.getInstance());
		if (!matchInternal(bound, actual)) {
			restore(before);
			return false;
		}
		addConstraint(variable, actual instanceof AnyType ? bound : actual);
		return true;
	}

	private boolean matchObjects(ObjectType expected, ObjectType actual) {
		Set<String> fieldNames = new TreeSet<>(expected.fields().keySet());
		fieldNames.addAll(actual.fields().keySet());
		for (String fieldName : fieldNames) {
			if (!matchInternal(effectiveFieldType(expected, fieldName), effectiveFieldType(actual, fieldName)))
				return false;
		}
		return matchInternal(expected.additionalFieldType(), actual.additionalFieldType());
	}

	/**
	 * Matches position by position, as {@link #matchObjects} matches field by field. A position one side
	 * knows and the other does not is compared against what that side says about the positions past its
	 * known ones, which is what lets {@code [NUMBER,NUMBER]} meet a scheme asking for
	 * {@code [*:NUMBER]} while an array of unknown length does not meet one asking for two numbers.
	 */
	private boolean matchArrays(ArrayType expected, ArrayType actual) {
		int positions = Math.max(expected.knownElements().size(), actual.knownElements().size());
		for (int i = 0; i < positions; i++) {
			if (!matchInternal(TypeRelations.elementAt(expected, i), TypeRelations.elementAt(actual, i)))
				return false;
		}
		return matchInternal(expected.additionalElementType(), actual.additionalElementType());
	}

	private static Type effectiveFieldType(ObjectType object, String fieldName) {
		Type declared = object.fields().get(fieldName);
		return declared != null ? declared : UnionType.of(object.additionalFieldType(), UndefinedType.getInstance());
	}

	private boolean matchRecursive(RecursiveType expected, RecursiveType actual) {
		expectedRecursiveVariables.push(expected.variable());
		actualRecursiveVariables.push(actual.variable());
		try {
			return matchInternal(expected.body(), actual.body());
		} finally {
			expectedRecursiveVariables.pop();
			actualRecursiveVariables.pop();
		}
	}

	private void addConstraint(TypeVariable variable, Type candidate) {
		Type previous = substitutions.get(variable);
		substitutions.put(variable, previous == null ? candidate : UnionType.of(previous, candidate));
	}

	private void merge(Map<TypeVariable, Type> match) {
		for (Map.Entry<TypeVariable, Type> substitution : match.entrySet())
			addConstraint(substitution.getKey(), substitution.getValue());
	}

	private void restore(Map<TypeVariable, Type> snapshot) {
		substitutions.clear();
		substitutions.putAll(snapshot);
	}

	private static int binderIndex(Deque<TypeVariable> binders, Type type) {
		if (!(type instanceof TypeVariable variable))
			return -1;
		@Var
		int index = 0;
		for (TypeVariable binder : binders) {
			if (binder.equals(variable))
				return index;
			index++;
		}
		return -1;
	}
}
