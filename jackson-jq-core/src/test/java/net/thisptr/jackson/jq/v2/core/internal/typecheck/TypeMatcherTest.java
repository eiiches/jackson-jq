package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;

class TypeMatcherTest {
	@Test
	void infersVariablesInsideObjectFields() {
		TypeVariable value = TypeVariable.of("Value");
		TypeMatcher matcher = new TypeMatcher(Set.of(value));

		assertThat(matcher.match(
				ObjectType.of("value", value),
				ObjectType.of("value", StringType.getInstance()))).isTrue();
		assertThat(matcher.validateBounds()).isTrue();
		assertThat(matcher.substitute(value)).isSameAs(StringType.getInstance());
	}

	@Test
	void mergesRepeatedAndUnionConstraints() {
		TypeVariable value = TypeVariable.of("Value");
		TypeMatcher objectMatcher = new TypeMatcher(Set.of(value));
		assertThat(objectMatcher.match(
				ObjectType.of("left", value, "right", value),
				ObjectType.of("left", StringType.getInstance(), "right", NumericType.getInstance()))).isTrue();
		assertThat(objectMatcher.substitute(value)).isEqualTo(UnionType.of(StringType.getInstance(), NumericType.getInstance()));

		TypeMatcher unionMatcher = new TypeMatcher(Set.of(value));
		assertThat(unionMatcher.match(
				UnionType.of(ArrayType.of(value), NullType.getInstance()),
				UnionType.of(ArrayType.of(StringType.getInstance()), ArrayType.of(NumericType.getInstance()), NullType.getInstance()))).isTrue();
		assertThat(unionMatcher.substitute(value)).isEqualTo(UnionType.of(StringType.getInstance(), NumericType.getInstance()));
	}

	@Test
	void mergesConstraintsFromAmbiguousUnionBranches() {
		TypeVariable value = TypeVariable.of("Value");
		TypeMatcher matcher = new TypeMatcher(Set.of(value));

		assertThat(matcher.match(UnionType.of(value, StringType.getInstance()), StringType.getInstance())).isTrue();
		assertThat(matcher.substitute(value)).isSameAs(StringType.getInstance());
	}

	@Test
	void failedUnionAlternativesDoNotLeakConstraints() {
		TypeVariable value = TypeVariable.of("Value");
		Type expected = UnionType.of(ObjectType.of("a", value, "b", StringType.getInstance()), ObjectType.of("a", NumericType.getInstance(), "b", value));
		Type actual = ObjectType.of("a", BooleanType.getInstance(), "b", NumericType.getInstance());
		TypeMatcher matcher = new TypeMatcher(Set.of(value));

		assertThat(matcher.match(expected, actual)).isFalse();
		assertThat(matcher.substitute(value)).isSameAs(AnyType.getInstance());
	}

	@Test
	void matchesObjectWidthOptionalityAndAdditionalFields() {
		Type requiredString = ObjectType.of("value", StringType.getInstance());
		Type optionalString = ObjectType.of("value", UnionType.of(StringType.getInstance(), UndefinedType.getInstance()));
		Type emptyClosed = ObjectType.of();

		assertThat(TypeMatcher.accepts(requiredString, emptyClosed)).isFalse();
		assertThat(TypeMatcher.accepts(optionalString, emptyClosed)).isTrue();
		assertThat(TypeMatcher.accepts(emptyClosed,
				ObjectType.of("value", UndefinedType.getInstance()))).isTrue();
		assertThat(TypeMatcher.accepts(requiredString,
				ObjectType.of("extra", NumericType.getInstance(), "value", StringType.getInstance()))).isFalse();
		assertThat(TypeMatcher.accepts(ObjectType.of(Map.of("value", StringType.getInstance()), AnyType.getInstance()),
				ObjectType.of("extra", NumericType.getInstance(), "value", StringType.getInstance()))).isTrue();
		assertThat(TypeMatcher.accepts(ObjectType.of(Map.of(), StringType.getInstance()),
				ObjectType.of(Map.of(), NumericType.getInstance()))).isFalse();
	}

	@Test
	void infersFromAdditionalObjectFieldsWithoutUsingNever() {
		TypeVariable value = TypeVariable.of("Value");
		TypeMatcher matcher = new TypeMatcher(Set.of(value));

		assertThat(matcher.match(
				ObjectType.of(Map.of(), value),
				ObjectType.of("number", NumericType.getInstance(), "string", StringType.getInstance()))).isTrue();
		assertThat(matcher.substitute(value)).isEqualTo(UnionType.of(NumericType.getInstance(), StringType.getInstance()));
	}

	@Test
	void matchesRecursiveTypesModuloBinderNames() {
		TypeVariable value = TypeVariable.of("Value");
		TypeVariable expectedNode = TypeVariable.of("ExpectedNode");
		RecursiveType expected = RecursiveType.of(expectedNode, (Type) ObjectType.of(
				"next", UnionType.of(expectedNode, NullType.getInstance()),
				"value", value));
		TypeVariable actualNode = TypeVariable.of("ActualNode");
		RecursiveType actual = RecursiveType.of(actualNode, (Type) ObjectType.of(
				"next", UnionType.of(actualNode, NullType.getInstance()),
				"value", StringType.getInstance()));
		TypeMatcher matcher = new TypeMatcher(Set.of(value));

		assertThat(matcher.match(expected, actual)).isTrue();
		assertThat(matcher.substitute(value)).isSameAs(StringType.getInstance());
		assertThat(TypeMatcher.accepts(expected, RecursiveType.of(actualNode, (Type) ArrayType.of(actualNode)))).isFalse();
	}

	@Test
	void infersVariablesUsedByDependentUpperBounds() {
		TypeVariable element = TypeVariable.of("Element");
		TypeVariable array = TypeVariable.of("Array");
		TypeMatcher matcher = new TypeMatcher(Set.of(element, array), Map.of(array, ArrayType.of(element)));

		assertThat(matcher.match(array, ArrayType.of(StringType.getInstance()))).isTrue();
		assertThat(matcher.validateBounds()).isTrue();
		assertThat(matcher.substitute(element)).isSameAs(StringType.getInstance());
		assertThat(matcher.substitute(array)).isEqualTo(ArrayType.of(StringType.getInstance()));

		TypeVariable string = TypeVariable.of("String");
		TypeMatcher mismatch = new TypeMatcher(Set.of(string), Map.of(string, StringType.getInstance()));
		assertThat(mismatch.match(string, NumericType.getInstance())).isFalse();
		assertThat(mismatch.substitute(string)).isSameAs(StringType.getInstance());
	}

	@Test
	void handlesAnyNeverAndUndefinedAccordingToTheirValueDomains() {
		assertThat(TypeMatcher.accepts(AnyType.getInstance(), StringType.getInstance())).isTrue();
		assertThat(TypeMatcher.accepts(StringType.getInstance(), AnyType.getInstance())).isTrue();
		assertThat(TypeMatcher.accepts(StringType.getInstance(), NeverType.getInstance())).isTrue();
		assertThat(TypeMatcher.accepts(AnyType.getInstance(), UndefinedType.getInstance())).isFalse();
		assertThat(TypeMatcher.accepts(UndefinedType.getInstance(), AnyType.getInstance())).isFalse();
		assertThat(TypeMatcher.accepts(NeverType.getInstance(), AnyType.getInstance())).isFalse();
		assertThat(TypeMatcher.accepts(UnionType.of(AnyType.getInstance(), UndefinedType.getInstance()), UndefinedType.getInstance())).isTrue();
	}

	@Test
	void anArrayWithKnownPositionsMeetsASchemeAskingForItsElementType() {
		// A scheme is written without positions, so a tuple has to meet it; the reverse does not hold,
		// because an array of unknown length may not reach the position the scheme names.
		assertThat(TypeMatcher.accepts(ArrayType.of(NumericType.getInstance()), ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance()))))
				.isTrue();
		assertThat(TypeMatcher.accepts(ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance())), ArrayType.of(NumericType.getInstance())))
				.isFalse();
		assertThat(TypeMatcher.accepts(ArrayType.of(NumericType.getInstance()), ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()))))
				.isFalse();
	}

	@Test
	void aVariableUnderAnArrayTakesEveryPositionsType() {
		TypeVariable element = TypeVariable.of("T");
		TypeMatcher matcher = new TypeMatcher(Set.of(element));
		assertThat(matcher.match(ArrayType.of(element), ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance())))).isTrue();
		assertThat(matcher.substitute(element)).isEqualTo(UnionType.of(NumericType.getInstance(), StringType.getInstance()));
	}

	@Test
	void anArrayOfArraysAcceptsAnArrayOfTuples() {
		// What `combinations` declares, against what an array literal of arrays infers to.
		TypeVariable element = TypeVariable.of("T");
		TypeMatcher matcher = new TypeMatcher(Set.of(element));
		assertThat(matcher.match(ArrayType.of(ArrayType.of(element)),
				ArrayType.of(List.of(ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance())))))).isTrue();
		assertThat(matcher.substitute(element)).isSameAs(NumericType.getInstance());
		assertThat(TypeMatcher.accepts(ArrayType.of(ArrayType.of(AnyType.getInstance())),
				ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance())))).isFalse();
	}

	@Test
	void subtypingIsDirectedOrder() {
		assertThat(TypeMatcher.isSubtype(StringType.getInstance(), AnyType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(AnyType.getInstance(), StringType.getInstance())).isFalse();
		assertThat(TypeMatcher.isSubtype(NeverType.getInstance(), StringType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(StringType.getInstance(), NeverType.getInstance())).isFalse();
		assertThat(TypeMatcher.isSubtype(NumericType.of(NumberKind.INT), NumericType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(NumericType.getInstance(), NumericType.of(NumberKind.INT))).isFalse();
		assertThat(TypeMatcher.isSubtype(ArrayType.of(List.of()), ArrayType.of(AnyType.getInstance()))).isTrue();
		assertThat(TypeMatcher.isSubtype(ArrayType.of(AnyType.getInstance()), ArrayType.of(List.of()))).isFalse();
	}

	@Test
	void matchesKnownStringAndBooleanValues() {
		// Assignability is "could be": a known value fits the type it is an instance of, and that type
		// fits a known value because a string of unknown content may well be the one asked for.
		assertThat(TypeMatcher.accepts(StringType.getInstance(), StringType.of("a"))).isTrue();
		assertThat(TypeMatcher.accepts(StringType.of("a"), StringType.getInstance())).isTrue();
		assertThat(TypeMatcher.accepts(StringType.of("a"), StringType.of("a"))).isTrue();
		assertThat(TypeMatcher.accepts(StringType.of("a"), StringType.of("b"))).isFalse();
		assertThat(TypeMatcher.accepts(BooleanType.of(true), BooleanType.of(false))).isFalse();
		assertThat(TypeMatcher.accepts(StringType.of("a"), NumericType.getInstance())).isFalse();
		assertThat(TypeMatcher.accepts(NumericType.of(1), NumericType.of(1))).isTrue();
		assertThat(TypeMatcher.accepts(NumericType.of(1), NumericType.of(2))).isFalse();
		assertThat(TypeMatcher.accepts(NumericType.of(1), NumericType.of(NumberKind.INT))).isTrue();
		assertThat(TypeMatcher.accepts(NumericType.of(NumberKind.INT), NumericType.of(1))).isTrue();

		// Subtyping is "is": only one of those directions is proven.
		assertThat(TypeMatcher.isSubtype(StringType.of("a"), StringType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(StringType.getInstance(), StringType.of("a"))).isFalse();
		assertThat(TypeMatcher.isSubtype(BooleanType.of(true), BooleanType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(BooleanType.getInstance(), BooleanType.of(true))).isFalse();
		assertThat(TypeMatcher.isSubtype(StringType.of("a"), StringType.of("b"))).isFalse();
		assertThat(TypeMatcher.isSubtype(NumericType.of(1), NumericType.of(NumberKind.INT))).isTrue();
		assertThat(TypeMatcher.isSubtype(NumericType.of(1), NumericType.getInstance())).isTrue();
		assertThat(TypeMatcher.isSubtype(NumericType.of(NumberKind.INT), NumericType.of(1))).isFalse();
		assertThat(TypeMatcher.isSubtype(NumericType.of(1), NumericType.of(2))).isFalse();
		assertThat(TypeMatcher.isSubtype(NumericType.of(1), NumericType.of(NumberKind.FLOAT))).isFalse();
	}

	@Test
	void aVariableUnderAnEmptyArrayBindsToNever() {
		TypeVariable element = TypeVariable.of("T");
		TypeMatcher matcher = new TypeMatcher(Set.of(element));
		assertThat(matcher.match(ArrayType.of(element), ArrayType.of(List.of()))).isTrue();
		assertThat(matcher.substitute(element)).isSameAs(NeverType.getInstance());
	}
}
