package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TypeTest {
	@Test
	void testScalarTypes() {
		assertThat(AnyType.getInstance()).isInstanceOf(AnyType.class).hasToString("ANY");
		assertThat(StringType.getInstance()).isInstanceOf(StringType.class).hasToString("STRING");
		assertThat(NumericType.getInstance()).isInstanceOf(NumericType.class).hasToString("NUMBER");
		assertThat(BooleanType.getInstance()).isInstanceOf(BooleanType.class).hasToString("BOOLEAN");
		assertThat(BinaryType.getInstance()).isInstanceOf(BinaryType.class).hasToString("BINARY");
		assertThat(NullType.getInstance()).isInstanceOf(NullType.class).hasToString("NULL");
		assertThat(NeverType.getInstance()).isInstanceOf(NeverType.class).hasToString("NEVER");
		assertThat(UndefinedType.getInstance()).isInstanceOf(UndefinedType.class).hasToString("UNDEFINED");

		assertThat(NullType.getInstance()).isNotEqualTo(UndefinedType.getInstance());
	}

	@Test
	void testNumericTypes() {
		assertThat(NumericType.getInstance()).isSameAs(NumericType.getInstance());
		assertThat(NumericType.getInstance().numberKind()).isEqualTo(NumberKind.UNKNOWN);
		assertThat(NumericType.of(NumberKind.INT).numberKind()).isEqualTo(NumberKind.INT);
		assertThat(NumericType.of(NumberKind.INT)).hasToString("INT");
		assertThat(NumericType.of(NumberKind.FLOAT)).hasToString("FLOAT");
		assertThat(NumericType.of(NumberKind.UNKNOWN)).isSameAs(NumericType.getInstance()).hasToString("NUMBER");
		// One instance per kind, so equality is identity.
		assertThat(NumericType.of(NumberKind.INT)).isSameAs(NumericType.of(NumberKind.INT));
		assertThat(NumericType.of(NumberKind.INT)).isNotEqualTo(NumericType.of(NumberKind.FLOAT));
		assertThat(NumericType.of(NumberKind.INT)).isNotEqualTo(NumericType.getInstance());
	}

	// NullAway checks null arguments; this assertion verifies runtime rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullNumericType() {
		assertThatThrownBy(() -> NumericType.of(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testNumberKindsCollapseInAUnion() {
		// A kind is a hint, so a union of several of them says no more than NUMBER does on its own.
		assertThat(UnionType.of(NumericType.of(NumberKind.INT), NumericType.of(NumberKind.FLOAT))).isSameAs(NumericType.getInstance());
		assertThat(UnionType.of(NumericType.of(NumberKind.INT), NumericType.getInstance())).isSameAs(NumericType.getInstance());
		assertThat(UnionType.of(NumericType.of(NumberKind.INT), NumericType.of(NumberKind.INT))).isSameAs(NumericType.of(NumberKind.INT));
		assertThat(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance())).hasToString("INT|STRING");
		assertThat(UnionType.of(NumericType.of(NumberKind.INT), NumericType.of(NumberKind.FLOAT), StringType.getInstance())).hasToString("NUMBER|STRING");
	}

	@Test
	void testArrayTypes() {
		ArrayType strings = ArrayType.of(StringType.getInstance());
		assertThat(strings.elementType()).isSameAs(StringType.getInstance());
		assertThat(strings).hasToString("[*:STRING]");
		assertThat(ArrayType.of(strings)).hasToString("[*:[*:STRING]]");
		assertThat(ArrayType.of(AnyType.getInstance())).isEqualTo(ArrayType.of(AnyType.getInstance()))
				.hasSameHashCodeAs(ArrayType.of(AnyType.getInstance()));
		assertThat(ArrayType.of(AnyType.getInstance())).isNotEqualTo(ArrayType.of(StringType.getInstance()));
	}

	// NullAway checks null arguments; these assertions verify runtime rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullArrayElementType() {
		assertThatThrownBy(() -> ArrayType.of((Type) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ArrayType.of((List<Type>) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ArrayType.of(Collections.singletonList(null)))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ArrayType.of(List.of(StringType.getInstance()), null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testKnownArrayElements() {
		ArrayType pair = ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance()));
		assertThat(pair.knownElements()).containsExactly(NumericType.getInstance(), NullType.getInstance());
		assertThat(pair.additionalElementType()).isSameAs(NeverType.getInstance());
		assertThat(pair.isClosed()).isTrue();
		assertThat(pair.elementType()).isEqualTo(UnionType.of(NumericType.getInstance(), NullType.getInstance()));
		assertThat(pair).hasToString("[NUMBER,NULL]");

		ArrayType open = ArrayType.of(List.of(NumericType.getInstance()), StringType.getInstance());
		assertThat(open.isClosed()).isFalse();
		assertThat(open).hasToString("[NUMBER,*:STRING]");
		assertThat(open.elementType()).isEqualTo(UnionType.of(NumericType.getInstance(), StringType.getInstance()));

		assertThat(ArrayType.of(List.of())).isEqualTo(ArrayType.of(NeverType.getInstance())).hasToString("[]");
		assertThat(pair).isEqualTo(ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance())))
				.hasSameHashCodeAs(ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance())));
		assertThat(pair).isNotEqualTo(ArrayType.of(List.of(NullType.getInstance(), NumericType.getInstance())));
	}

	@Test
	void testKnownArrayElementsSayNothingAboutOtherPositions() {
		// A known element is a claim about one position; the element type is a claim about all of them.
		ArrayType pair = ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()));
		assertThat(pair).isNotEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
		// UNDEFINED is how a position says it may be absent, so it is never part of the element type.
		assertThat(ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()))).elementType())
				.isSameAs(NumericType.getInstance());
	}

	@Test
	void testArrayNormalization() {
		// A required element cannot follow an optional element.
		assertThatThrownBy(() -> ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()), StringType.getInstance())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("A required element cannot follow an optional element");
		// A trailing position saying no more than the additional element type is redundant.
		assertThat(ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance())), NumericType.getInstance()))
				.isEqualTo(ArrayType.of(NumericType.getInstance()));
		assertThat(ArrayType.of(List.of(NumericType.getInstance(), UndefinedType.getInstance()))).isEqualTo(ArrayType.of(List.of(NumericType.getInstance())));
		// Asserting that a position is present is not redundant: it bounds the length from below.
		assertThat(ArrayType.of(List.of(NumericType.getInstance()), NumericType.getInstance())).isNotEqualTo(ArrayType.of(NumericType.getInstance()));
	}

	@Test
	void testObjectTypes() {
		Map<String, Type> fields = new LinkedHashMap<>();
		fields.put("name", StringType.getInstance());
		fields.put("age", NumericType.of(NumberKind.INT));
		ObjectType object = ObjectType.of(fields);

		assertThat(object.fields()).containsExactly(
				Map.entry("age", NumericType.of(NumberKind.INT)),
				Map.entry("name", StringType.getInstance()));
		assertThat(object).hasToString("{age:INT,name:STRING}");
		assertThat(ObjectType.of(Collections.<String, Type>emptyMap())).hasToString("{}");
		assertThat(object).isEqualTo(ObjectType.of(new HashMap<>(fields)))
				.hasSameHashCodeAs(ObjectType.of(new HashMap<>(fields)));

		fields.put("active", BooleanType.getInstance());
		assertThat(object.fields()).containsOnlyKeys("age", "name");
		assertThatThrownBy(() -> object.fields().put("x", AnyType.getInstance())).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void testOpenObjectTypes() {
		ObjectType object = ObjectType.of(Map.of("name", StringType.getInstance()), AnyType.getInstance());
		assertThat(object.isClosed()).isFalse();
		assertThat(object.additionalFieldType()).isSameAs(AnyType.getInstance());
		assertThat(object).hasToString("{name:STRING,*:ANY}");
		assertThat(ObjectType.of(AnyType.getInstance())).isEqualTo(ObjectType.of(Map.of(), AnyType.getInstance())).hasToString("{*:ANY}");
		assertThat(ObjectType.of(AnyType.getInstance())).isEqualTo(ObjectType.of(AnyType.getInstance()));
		assertThat(ObjectType.of().isClosed()).isTrue();
	}

	@Test
	void testTypeVariablesAreValues() {
		// The same name denotes the same variable, so an occurrence may be written anew.
		assertThat(TypeVariable.of("T")).isEqualTo(TypeVariable.of("T")).hasSameHashCodeAs(TypeVariable.of("T"));
		assertThat(TypeVariable.of("T")).isNotEqualTo(TypeVariable.of("U"));
		assertThat(UnionType.of(TypeVariable.of("T"), TypeVariable.of("T"))).isEqualTo(TypeVariable.of("T"));
		assertThat(ArrayType.of(TypeVariable.of("T"))).isEqualTo(ArrayType.of(TypeVariable.of("T")));
	}

	@Test
	void testTypeVariablesAndSchemes() {
		TypeVariable t = TypeVariable.of("T");
		Type bound = UnionType.of(StringType.getInstance(), ArrayType.of(AnyType.getInstance()));
		TypeScheme<FilterType> scheme = TypeScheme.of(Map.of(t, bound), FilterType.of(t, t));
		assertThat(scheme.typeVariables()).containsEntry(t, bound);
		assertThat(scheme.body()).isEqualTo(FilterType.of(t, t));
		assertThat(scheme).hasToString("<T: STRING|[*:ANY]> T -> T");
		assertThat(TypeScheme.of(Map.of(TypeVariable.of("T"), AnyType.getInstance()), FilterType.of(AnyType.getInstance(), AnyType.getInstance())))
				.hasToString("<T> ANY -> ANY");
		Map<TypeVariable, Type> tu = new LinkedHashMap<>();
		tu.put(TypeVariable.of("T"), AnyType.getInstance());
		tu.put(TypeVariable.of("U"), AnyType.getInstance());
		assertThat(TypeScheme.of(tu, FilterType.of(TypeVariable.of("T"), TypeVariable.of("U"))))
				.hasToString("<T, U> T -> U");
	}

	@Test
	void testFunctionTypes() {
		FilterType predicate = FilterType.of(TypeVariable.of("T"), BooleanType.getInstance());
		FunctionType select = FunctionType.of(ArrayType.of(TypeVariable.of("T")),
				ArrayType.of(TypeVariable.of("T")), predicate);

		assertThat(FunctionType.of(AnyType.getInstance(), StringType.getInstance()))
				.hasToString("() => (ANY -> STRING)");
		assertThat(select).hasToString("(T -> BOOLEAN) => ([*:T] -> [*:T])");
		assertThat(FunctionType.of(AnyType.getInstance(), BooleanType.getInstance(), predicate, FilterType.of(StringType.getInstance(), NumericType.getInstance())))
				.hasToString("(T -> BOOLEAN; STRING -> NUMBER) => (ANY -> BOOLEAN)");
		assertThat(TypeScheme.of(Map.of(TypeVariable.of("T"), AnyType.getInstance()), select))
				.hasToString("<T> (T -> BOOLEAN) => ([*:T] -> [*:T])");
	}

	// NullAway checks null arguments; these assertions verify runtime rejection.
	@SuppressWarnings("NullAway")
	@Test
	void testFunctionTypeOfOverload() {
		FilterType param = FilterType.of(StringType.getInstance(), NumericType.getInstance());
		FunctionType fromFilterType = FunctionType.of(FilterType.of(AnyType.getInstance(), BooleanType.getInstance()), param);
		FunctionType fromTypes = FunctionType.of(AnyType.getInstance(), BooleanType.getInstance(), param);

		assertThat(fromTypes).isEqualTo(fromFilterType);
		assertThat(fromTypes.returnType()).isEqualTo(FilterType.of(AnyType.getInstance(), BooleanType.getInstance()));
		assertThat(fromTypes.parameterTypes()).containsExactly(param);

		FunctionType nullary = FunctionType.of(NumericType.getInstance(), StringType.getInstance());
		assertThat(nullary).isEqualTo(FunctionType.of(FilterType.of(NumericType.getInstance(), StringType.getInstance())));
		assertThat(nullary.parameterTypes()).isEmpty();

		assertThatThrownBy(() -> FunctionType.of((Type) null, StringType.getInstance()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> FunctionType.of(StringType.getInstance(), (Type) null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> FunctionType.of(StringType.getInstance(), NumericType.getInstance(), (FilterType[]) null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void testTypeSchemesRejectDuplicateAndFreeVariables() {
		TypeVariable t = TypeVariable.of("T");
		TypeVariable u = TypeVariable.of("U");

		assertThatThrownBy(() -> TypeScheme.of(FilterType.of(t, t)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("free type variables").hasMessageContaining("T");
		assertThatThrownBy(() -> TypeScheme.of(Map.of(t, AnyType.getInstance()), FilterType.of(t, u)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("free type variables").hasMessageContaining("U");
		assertThatThrownBy(() -> TypeScheme.of(Map.of(t, AnyType.getInstance()), FunctionType.of(t, t, FilterType.of(t, u))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("free type variables").hasMessageContaining("U");
	}

	@Test
	void testTypeSchemeVariablesInUpperBoundsMustBeQuantified() {
		TypeVariable upper = TypeVariable.of("Upper");
		TypeVariable value = TypeVariable.of("Value");

		assertThatThrownBy(() -> TypeScheme.of(Map.of(value, upper), FilterType.of(value, value)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("free type variables").hasMessageContaining("Upper");
		Map<TypeVariable, Type> bounds = new LinkedHashMap<>();
		bounds.put(upper, AnyType.getInstance());
		bounds.put(value, upper);
		assertThat(TypeScheme.of(bounds, FilterType.of(value, value)).typeVariables().keySet())
				.containsExactly(upper, value);
	}

	@Test
	void testRecursiveVariablesAreBoundWithinTypeSchemes() {
		TypeVariable recursiveVariable = TypeVariable.of("Recursive");
		RecursiveType recursive = RecursiveType.of(recursiveVariable, ArrayType.of(recursiveVariable));

		assertThat(TypeScheme.of(FilterType.of(recursive, recursive)).typeVariables()).isEmpty();

		TypeVariable value = TypeVariable.of("Value");
		RecursiveType withFreeVariable = RecursiveType.of(recursiveVariable, ObjectType.of("next", recursiveVariable, "value", value));
		assertThatThrownBy(() -> TypeScheme.of(FilterType.of(withFreeVariable, withFreeVariable)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("free type variables").hasMessageContaining("Value");
		assertThat(TypeScheme.of(Map.of(value, AnyType.getInstance()), FilterType.of(withFreeVariable, withFreeVariable)).typeVariables().keySet())
				.containsExactly(value);
	}

	@Test
	void testRecursiveTypes() {
		TypeVariable t = TypeVariable.of("T");
		RecursiveType left = RecursiveType.of(t, UnionType.of(NullType.getInstance(), ArrayType.of(t)));

		assertThat(left).hasToString("RECURSIVE<T = NULL|[*:T]>");
		assertThat(left.variable()).isEqualTo(((ArrayType) ((UnionType) left.body()).alternatives().get(1)).elementType());
		TypeVariable variable = TypeVariable.of("T");
		TypeVariable variable1 = TypeVariable.of("T");
		assertThat(left).isEqualTo(RecursiveType.of(variable1, UnionType.of(NullType.getInstance(), ArrayType.of(TypeVariable.of("T")))))
				.hasSameHashCodeAs(RecursiveType.of(variable, UnionType.of(NullType.getInstance(), ArrayType.of(TypeVariable.of("T")))));
		// equals is structural, so the name of the bound variable is significant.
		TypeVariable node = TypeVariable.of("Node");
		assertThat(left).isNotEqualTo(RecursiveType.of(node, UnionType.of(NullType.getInstance(), ArrayType.of(node))));
	}

	@Test
	void testRecursiveTypesMustBeUsedAndGuarded() {
		TypeVariable t = TypeVariable.of("T");
		assertThatThrownBy(() -> RecursiveType.of(t, StringType.getInstance()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reference");
		assertThatThrownBy(() -> RecursiveType.of(t, UnionType.of(t, StringType.getInstance())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("array or object");
		// The body must reference the binder itself, not a variable an inner binder shadows.
		assertThatThrownBy(() -> RecursiveType.of(t, ArrayType.of(RecursiveType.of(t, ArrayType.of(t)))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reference");
	}


	@Test
	void testOptionalObjectFields() {
		Map<String, Type> fields = new LinkedHashMap<>();
		fields.put("optional", UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()));
		fields.put("absent", UndefinedType.getInstance());

		ObjectType object = ObjectType.of(fields);
		assertThat(object.fields().get("optional")).isEqualTo(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()));
		assertThat(object.fields().get("absent")).isSameAs(UndefinedType.getInstance());
		assertThat(object).hasToString("{absent:UNDEFINED,optional:NUMBER|UNDEFINED}");
	}

	@Test
	void testObjectFieldNameEscaping() {
		Map<String, Type> fields = Collections.singletonMap("a\"b\\c\n", StringType.getInstance());
		assertThat(ObjectType.of(fields)).hasToString("{\"a\\\"b\\\\c\\n\":STRING}");
	}

	// NullAway checks null arguments; these assertions verify runtime rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testInvalidObjectTypes() {
		assertThatThrownBy(() -> ObjectType.of((Type) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ObjectType.of((Map<String, ? extends Type>) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ObjectType.of((String) null, StringType.getInstance())).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> ObjectType.of("name", (Type) null)).isInstanceOf(NullPointerException.class);

		Map<String, Type> nullKey = new HashMap<>();
		nullKey.put(null, StringType.getInstance());
		assertThatThrownBy(() -> ObjectType.of(nullKey)).isInstanceOf(NullPointerException.class);

		Map<String, Type> nullValue = new HashMap<>();
		nullValue.put("name", null);
		assertThatThrownBy(() -> ObjectType.of(nullValue)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testObjectTypeOverloads() {
		assertThat(ObjectType.of()).hasToString("{}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance())).hasToString("{k1:BOOLEAN}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance())).hasToString("{k1:BOOLEAN,k2:STRING}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance())).hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance(),
				"k6", NeverType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY,k6:NEVER}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance(),
				"k6", NeverType.getInstance(), "k7", UndefinedType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY,k6:NEVER,k7:UNDEFINED}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance(),
				"k6", NeverType.getInstance(), "k7", UndefinedType.getInstance(), "k8", BooleanType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY,k6:NEVER,k7:UNDEFINED,k8:BOOLEAN}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance(),
				"k6", NeverType.getInstance(), "k7", UndefinedType.getInstance(), "k8", BooleanType.getInstance(), "k9", StringType.getInstance()))
				.hasToString("{k1:BOOLEAN,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY,k6:NEVER,k7:UNDEFINED,k8:BOOLEAN,k9:STRING}");
		assertThat(ObjectType.of("k1", BooleanType.getInstance(), "k2", StringType.getInstance(), "k3", NullType.getInstance(), "k4", NumericType.getInstance(), "k5", AnyType.getInstance(),
				"k6", NeverType.getInstance(), "k7", UndefinedType.getInstance(), "k8", BooleanType.getInstance(), "k9", StringType.getInstance(), "k10", NullType.getInstance()))
				.hasToString("{k1:BOOLEAN,k10:NULL,k2:STRING,k3:NULL,k4:NUMBER,k5:ANY,k6:NEVER,k7:UNDEFINED,k8:BOOLEAN,k9:STRING}");

		assertThatThrownBy(() -> ObjectType.of("k1", BooleanType.getInstance(), "k1", StringType.getInstance()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testUnionNormalization() {
		Type nullOrString = UnionType.of(StringType.getInstance(), NullType.getInstance());
		assertThat(nullOrString).isInstanceOf(UnionType.class).hasToString("NULL|STRING");
		assertThat(nullOrString).isEqualTo(UnionType.of(NullType.getInstance(), StringType.getInstance()))
				.hasSameHashCodeAs(UnionType.of(NullType.getInstance(), StringType.getInstance()));

		Type flattened = UnionType.of(BooleanType.getInstance(), nullOrString, StringType.getInstance());
		assertThat(flattened).hasToString("BOOLEAN|NULL|STRING");
		assertThat(flattened).isEqualTo(UnionType.of(Arrays.asList(StringType.getInstance(), BooleanType.getInstance(), NullType.getInstance())));
		assertThat(UnionType.of(StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(UnionType.of(StringType.getInstance(), StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(UnionType.of()).isSameAs(NeverType.getInstance());
		assertThat(UnionType.of(Collections.<Type>emptyList())).isSameAs(NeverType.getInstance());
		assertThat(UnionType.of(NeverType.getInstance(), StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(UnionType.of(StringType.getInstance(), AnyType.getInstance(), NullType.getInstance())).isSameAs(AnyType.getInstance());

		Type anyOrUndefined = UnionType.of(AnyType.getInstance(), UndefinedType.getInstance());
		assertThat(anyOrUndefined).hasToString("ANY|UNDEFINED");
		assertThat(UnionType.of(AnyType.getInstance(), NumericType.getInstance(), UndefinedType.getInstance())).isEqualTo(anyOrUndefined);
		assertThat(UnionType.of(NumericType.getInstance(), anyOrUndefined)).isEqualTo(anyOrUndefined);
	}

	@Test
	void testUnionAlternativesAreImmutable() {
		UnionType union = (UnionType) UnionType.of(StringType.getInstance(), NullType.getInstance());
		assertThat(union.alternatives()).containsExactly(NullType.getInstance(), StringType.getInstance());
		assertThatThrownBy(() -> union.alternatives().add(BooleanType.getInstance())).isInstanceOf(UnsupportedOperationException.class);
	}

	// NullAway checks null arguments; these assertions verify runtime rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testInvalidUnions() {
		assertThatThrownBy(() -> UnionType.of((Type[]) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> UnionType.of((Iterable<Type>) null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> UnionType.of(StringType.getInstance(), null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> UnionType.of(AnyType.getInstance(), null)).isInstanceOf(NullPointerException.class);
		List<Type> alternatives = Arrays.asList(StringType.getInstance(), null);
		assertThatThrownBy(() -> UnionType.of(alternatives)).isInstanceOf(NullPointerException.class);
	}
}
