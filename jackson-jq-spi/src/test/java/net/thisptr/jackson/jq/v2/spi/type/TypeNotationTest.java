package net.thisptr.jackson.jq.v2.spi.type;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TypeNotationTest {
	private static final TypeVariable T = TypeVariable.of("T");
	private static final TypeVariable U = TypeVariable.of("U");

	/**
	 * A field name needing every escape the notation defines, including a C0 control.
	 */
	private static final String ESCAPED_FIELD_NAME = "a\"b\\c\n\r\t\b\f" + (char) 1;

	private static List<Type> roundTripTypes() {
		Map<String, Type> optionalFields = new LinkedHashMap<>();
		optionalFields.put("absent", UndefinedType.getInstance());
		optionalFields.put("optional", UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()));
		return List.of(
				AnyType.getInstance(), NeverType.getInstance(), UndefinedType.getInstance(), NullType.getInstance(), BooleanType.getInstance(), StringType.getInstance(), BinaryType.getInstance(),
				NumericType.getInstance(), NumericType.of(NumberKind.INT), NumericType.of(NumberKind.FLOAT),
				ArrayType.of(NeverType.getInstance()),
				ArrayType.of(StringType.getInstance()),
				ArrayType.of(ArrayType.of(StringType.getInstance())),
				ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance())),
				ArrayType.of(List.of(NumericType.getInstance()), StringType.getInstance()),
				ArrayType.of(List.of(NumericType.getInstance(), UnionType.of(StringType.getInstance(), UndefinedType.getInstance()))),
				ObjectType.of(),
				ObjectType.of(AnyType.getInstance()),
				ObjectType.of("age", NumericType.of(NumberKind.INT), "name", StringType.getInstance()),
				ObjectType.of(Map.of("name", StringType.getInstance()), AnyType.getInstance()),
				ObjectType.of(optionalFields),
				ObjectType.of(ESCAPED_FIELD_NAME, StringType.getInstance()),
				ObjectType.of("a-b", StringType.getInstance(), "", NullType.getInstance(), "*", BooleanType.getInstance()),
				StringType.of("number"), StringType.of(""), StringType.of(ESCAPED_FIELD_NAME),
				BooleanType.of(true), BooleanType.of(false),
				UnionType.of(StringType.of("a"), StringType.of("b"), NullType.getInstance()),
				ArrayType.of(StringType.of("a")),
				ObjectType.of("kind", StringType.of("a"), "ok", BooleanType.of(true)),
				UnionType.of(NullType.getInstance(), StringType.getInstance()),
				UnionType.of(BooleanType.getInstance(), NullType.getInstance(), StringType.getInstance()),
				UnionType.of(AnyType.getInstance(), UndefinedType.getInstance()),
				ObjectType.of(Map.of("items", ArrayType.of(ObjectType.of(Map.of("id", NumericType.getInstance()), AnyType.getInstance())))),
				RecursiveType.of(T, UnionType.of(NullType.getInstance(), ArrayType.of(T))),
				RecursiveType.of(T, ObjectType.of(Map.of("next", UnionType.of(T, UndefinedType.getInstance())), AnyType.getInstance())),
				// A recursive type below another binder, so the inner '>' closes the inner one.
				RecursiveType.of(T, ArrayType.of(RecursiveType.of(U, ArrayType.of(UnionType.of(U, T))))));
	}

	@ParameterizedTest
	@MethodSource("roundTripTypes")
	void printedTypesParseBackToThemselves(Type type) {
		assertThat(Type.valueOf(type.toString())).isEqualTo(type);
		// Printing is canonical, so a second trip cannot drift either.
		assertThat(Type.valueOf(type.toString()).toString()).isEqualTo(type.toString());
	}

	@Test
	void printsTheAgreedSpelling() {
		assertThat(NumericType.of(NumberKind.INT)).hasToString("INT");
		assertThat(ArrayType.of(StringType.getInstance())).hasToString("[*:STRING]");
		assertThat(ArrayType.of(NeverType.getInstance())).hasToString("[]");
		assertThat(ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance()))).hasToString("[NUMBER,NULL]");
		assertThat(ArrayType.of(List.of(NumericType.getInstance()), StringType.getInstance())).hasToString("[NUMBER,*:STRING]");
		assertThat(ObjectType.of()).hasToString("{}");
		assertThat(ObjectType.of(AnyType.getInstance())).hasToString("{*:ANY}");
		assertThat(ObjectType.of("age", NumericType.of(NumberKind.INT))).hasToString("{age:INT}");
		assertThat(ObjectType.of("a-b", StringType.getInstance())).hasToString("{\"a-b\":STRING}");
		assertThat(RecursiveType.of(T, ArrayType.of(T))).hasToString("RECURSIVE<T = [*:T]>");
		assertThat(FilterType.of(AnyType.getInstance(), StringType.getInstance())).hasToString("ANY -> STRING");
		assertThat(StringType.of("number")).hasToString("\"number\"");
		assertThat(BooleanType.of(true)).hasToString("true");
		assertThat(BooleanType.of(false)).hasToString("false");
		assertThat(ObjectType.of("kind", StringType.of("a"))).hasToString("{kind:\"a\"}");
	}

	@Test
	void parsesKnownStringsAndBooleans() {
		assertThat(Type.valueOf("\"number\"")).isEqualTo(StringType.of("number"));
		assertThat(Type.valueOf("\"\"")).isEqualTo(StringType.of(""));
		assertThat(Type.valueOf("true")).isSameAs(BooleanType.of(true));
		assertThat(Type.valueOf("false")).isSameAs(BooleanType.of(false));
		assertThat(Type.valueOf("\"a\\u0001b\"")).isEqualTo(StringType.of("a\u0001b"));
		// A quoted string in field position is still a field name, so the two uses do not collide.
		assertThat(Type.valueOf("{\"a-b\":\"a\"}")).isEqualTo(ObjectType.of("a-b", StringType.of("a")));
		assertThat(Type.valueOf("\"a\"|\"b\"")).isEqualTo(UnionType.of(StringType.of("a"), StringType.of("b")));
	}

	@Test
	void parsesEveryScalarAndNumberKind() {
		assertThat(Type.valueOf("ANY")).isSameAs(AnyType.getInstance());
		assertThat(Type.valueOf("NEVER")).isSameAs(NeverType.getInstance());
		assertThat(Type.valueOf("UNDEFINED")).isSameAs(UndefinedType.getInstance());
		assertThat(Type.valueOf("NULL")).isSameAs(NullType.getInstance());
		assertThat(Type.valueOf("BOOLEAN")).isSameAs(BooleanType.getInstance());
		assertThat(Type.valueOf("STRING")).isSameAs(StringType.getInstance());
		assertThat(Type.valueOf("BINARY")).isSameAs(BinaryType.getInstance());
		assertThat(Type.valueOf("NUMBER")).isSameAs(NumericType.getInstance());
		assertThat(Type.valueOf("INT")).isSameAs(NumericType.of(NumberKind.INT));
		assertThat(Type.valueOf("FLOAT")).isSameAs(NumericType.of(NumberKind.FLOAT));
	}

	@Test
	void distinguishesAHomogeneousArrayFromAOneElementTuple() {
		assertThat(Type.valueOf("[*:STRING]")).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(Type.valueOf("[STRING]")).isEqualTo(ArrayType.of(List.of(StringType.getInstance())));
		assertThat(Type.valueOf("[*:STRING]")).isNotEqualTo(Type.valueOf("[STRING]"));
	}

	@Test
	void roundTripsAUnionOfTwoRecursiveTypes() {
		// The old notation could not: `mu X. body` had no closing delimiter, so the first body
		// swallowed the `|` and every alternative after it.
		TypeVariable variable1 = TypeVariable.of("X");
		Type left = RecursiveType.of(variable1, ArrayType.of(TypeVariable.of("X")));
		TypeVariable variable = TypeVariable.of("Y");
		Type right = RecursiveType.of(variable, ObjectType.of(Map.of("next", TypeVariable.of("Y")), AnyType.getInstance()));
		Type union = UnionType.of(left, right);

		assertThat(union).isInstanceOf(UnionType.class);
		assertThat(Type.valueOf(union.toString())).isEqualTo(union);
	}

	@Test
	void roundTripsARecursiveTypeBesideALowercaseVariable() {
		TypeVariable variable = TypeVariable.of("X");
		Type union = UnionType.of(RecursiveType.of(variable, ArrayType.of(TypeVariable.of("X"))), TypeVariable.of("x"));

		assertThat(union).isInstanceOf(UnionType.class);
		assertThat(Type.valueOf(union.toString())).isEqualTo(union);
	}

	@Test
	void acceptsNonCanonicalSpellings() {
		assertThat(Type.valueOf("[*:NEVER]")).isEqualTo(ArrayType.of(NeverType.getInstance())).hasToString("[]");
		assertThat(Type.valueOf("[NUMBER,*:NEVER]")).isEqualTo(ArrayType.of(List.of(NumericType.getInstance())))
				.hasToString("[NUMBER]");
		assertThat(Type.valueOf("{\"age\":INT}")).hasToString("{age:INT}");
		assertThat(Type.valueOf("STRING|STRING")).isSameAs(StringType.getInstance());
		assertThat(Type.valueOf("INT|FLOAT")).isSameAs(NumericType.getInstance());
		assertThat(Type.valueOf("NEVER|STRING")).isSameAs(StringType.getInstance());
		assertThat(Type.valueOf("STRING|ANY")).isSameAs(AnyType.getInstance());
	}

	@Test
	void ignoresWhitespaceBetweenTokens() {
		assertThat(Type.valueOf("  [ NUMBER , *: STRING ]  "))
				.isEqualTo(ArrayType.of(List.of(NumericType.getInstance()), StringType.getInstance()));
		assertThat(Type.valueOf("{\n\tage : INT ,\n\t*: ANY\n}"))
				.isEqualTo(ObjectType.of(Map.of("age", NumericType.of(NumberKind.INT)), AnyType.getInstance()));
		assertThat(Type.valueOf("NULL\t|\nSTRING")).isEqualTo(UnionType.of(NullType.getInstance(), StringType.getInstance()));
		assertThat(Type.valueOf("RECURSIVE < T = [ *: T ] >")).isEqualTo(RecursiveType.of(T, ArrayType.of(T)));
	}

	@Test
	void readsFieldNamesBareOrQuoted() {
		assertThat(Type.valueOf("{a:STRING,_b2:NULL}"))
				.isEqualTo(ObjectType.of("a", StringType.getInstance(), "_b2", NullType.getInstance()));
		// A field name may be a reserved word, since a type can never appear in key position.
		assertThat(Type.valueOf("{INT:STRING}")).isEqualTo(ObjectType.of("INT", StringType.getInstance()));
		assertThat(Type.valueOf("{\"a\\\"b\\\\c\\n\":STRING}"))
				.isEqualTo(ObjectType.of("a\"b\\c\n", StringType.getInstance()));
		assertThat(Type.valueOf("{\"\\u0041\\u0042\":STRING}"))
				.isEqualTo(ObjectType.of("AB", StringType.getInstance()));
		assertThat(Type.valueOf("{\"\\/\":STRING}")).isEqualTo(ObjectType.of("/", StringType.getInstance()));
	}

	@Test
	void parsesFilterAndFunctionTypes() {
		assertThat(FilterType.valueOf("ANY -> STRING")).isEqualTo(FilterType.of(AnyType.getInstance(), StringType.getInstance()));
		// A union binds tighter than the arrow, so neither side needs brackets.
		assertThat(FilterType.valueOf("NULL|STRING -> INT|BOOLEAN"))
				.isEqualTo(FilterType.of(UnionType.of(NullType.getInstance(), StringType.getInstance()),
						UnionType.of(NumericType.of(NumberKind.INT), BooleanType.getInstance())));
		assertThat(FunctionType.valueOf("() => (ANY -> STRING)"))
				.isEqualTo(FunctionType.of(AnyType.getInstance(), StringType.getInstance()));
		assertThat(FunctionType.valueOf("(T -> BOOLEAN; STRING -> NUMBER) => (ANY -> BOOLEAN)"))
				.isEqualTo(FunctionType.of(AnyType.getInstance(), BooleanType.getInstance(),
						FilterType.of(T, BooleanType.getInstance()), FilterType.of(StringType.getInstance(), NumericType.getInstance())));
	}

	@Test
	void roundTripsSignaturesAndSchemes() {
		FilterType filter = FilterType.of(UnionType.of(NullType.getInstance(), StringType.getInstance()), ArrayType.of(AnyType.getInstance()));
		assertThat(FilterType.valueOf(filter.toString())).isEqualTo(filter);

		FunctionType function = FunctionType.of(ArrayType.of(T), ArrayType.of(T),
				FilterType.of(T, BooleanType.getInstance()));
		assertThat(FunctionType.valueOf(function.toString())).isEqualTo(function);

		TypeScheme<FilterType> monomorphic = TypeScheme.of(filter);
		assertThat(TypeScheme.ofFilter(monomorphic.toString())).isEqualTo(monomorphic);

		TypeScheme<FunctionType> polymorphic = TypeScheme.of(Map.of(T, AnyType.getInstance()), function);
		assertThat(polymorphic).hasToString("<T> (T -> BOOLEAN) => ([*:T] -> [*:T])");
		assertThat(TypeScheme.ofFunction(polymorphic.toString())).isEqualTo(polymorphic);
	}

	@Test
	void recoversUpperBoundsFromTheirBinder() {
		TypeVariable bounded = TypeVariable.of("T");
		Type bound = UnionType.of(StringType.getInstance(), ArrayType.of(AnyType.getInstance()));
		TypeScheme<FilterType> scheme = TypeScheme.of(Map.of(bounded, bound), FilterType.of(bounded, bounded));

		assertThat(scheme).hasToString("<T: STRING|[*:ANY]> T -> T");
		assertThat(TypeScheme.ofFilter(scheme.toString())).isEqualTo(scheme);
		TypeScheme<FilterType> parsed = TypeScheme.ofFilter("<T: STRING> T -> T");
		assertThat(parsed.typeVariables()).containsEntry(TypeVariable.of("T"), StringType.getInstance());
		// A later binder's bound may name an earlier binder.
		TypeScheme<FilterType> dependent = TypeScheme.ofFilter("<T: STRING, U: [*:T]> T -> U");
		assertThat(dependent.typeVariables().keySet())
				.containsExactly(TypeVariable.of("T"), TypeVariable.of("U"));
		assertThat(dependent.typeVariables())
				.containsEntry(TypeVariable.of("T"), StringType.getInstance())
				.containsEntry(TypeVariable.of("U"), ArrayType.of(TypeVariable.of("T")));
	}

	@Test
	void parsesAVariableWithNoBinderAsUnbounded() {
		assertThat(Type.valueOf("[*:T]")).isEqualTo(ArrayType.of(TypeVariable.of("T")));
	}

	@Test
	void bindsTheRecursiveVariableOverItsBodyOnly() {
		assertThat(Type.valueOf("RECURSIVE<T = [*:T]>")).isEqualTo(RecursiveType.of(T, ArrayType.of(T)));
		// The inner binder shadows the outer one, so the inner body's T is the inner variable.
		Type shadowed = Type.valueOf("RECURSIVE<T = [*:T|RECURSIVE<U = [*:U]>]>");
		assertThat(shadowed).isInstanceOf(RecursiveType.class);
		assertThat(Type.valueOf(shadowed.toString())).isEqualTo(shadowed);
	}

	@Test
	void rejectsMalformedInput() {
		assertThatThrownBy(() -> Type.valueOf("[NUMBER|UNDEFINED,STRING]"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("A required element cannot follow an optional element");
		assertThatThrownBy(() -> Type.valueOf("[*:STRING,NUMBER]"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected ']'");
		assertThatThrownBy(() -> Type.valueOf("[*:STRING,*:NUMBER]"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected ']'");
		assertThatThrownBy(() -> Type.valueOf("{*:ANY,a:STRING}"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected '}'");
		assertThatThrownBy(() -> Type.valueOf("{a:STRING,a:NUMBER}"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate field: a");
		assertThatThrownBy(() -> Type.valueOf("[STRING"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected ']'");
		assertThatThrownBy(() -> Type.valueOf("RECURSIVE<T [*:T]>"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected '='");
		assertThatThrownBy(() -> Type.valueOf("STRING NUMBER"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unexpected trailing input");
		// Parentheses are not part of a type, which is what leaves a leading ( to mean a parameter list.
		assertThatThrownBy(() -> Type.valueOf("(STRING)"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected a type");
		assertThatThrownBy(() -> Type.valueOf("{a STRING}"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected ':'");
		assertThatThrownBy(() -> Type.valueOf("{\"a:STRING}"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unterminated string");
		assertThatThrownBy(() -> Type.valueOf(""))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected a type");
		assertThatThrownBy(() -> FilterType.valueOf("ANY => STRING"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected '->'");
		assertThatThrownBy(() -> FunctionType.valueOf("(ANY -> STRING, STRING -> ANY) => (ANY -> ANY)"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected ')'");
		assertThatThrownBy(() -> TypeScheme.ofFilter("<T: STRING, T> T -> T"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate type variable: T");
		assertThatThrownBy(() -> TypeScheme.ofFilter("<INT> ANY -> ANY"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reserved type name");
	}

	@Test
	void reportsTheOffsetAndTheInput() {
		assertThatThrownBy(() -> Type.valueOf("[NUMBER,*STRING]"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expected ':' at offset 9: [NUMBER,*STRING]");
	}

	@Test
	void rejectsVariableNamesThatWouldBeReadBackAsAKeyword() {
		assertThatThrownBy(() -> TypeVariable.of("INT")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved type name: INT");
		assertThatThrownBy(() -> TypeVariable.of("RECURSIVE")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved type name: RECURSIVE");
		assertThatThrownBy(() -> TypeVariable.of("true")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved type name: true");
		assertThatThrownBy(() -> TypeVariable.of("false")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved type name: false");
		// Matching is case-sensitive, so the conventional mixed-case names stay legal.
		assertThat(TypeVariable.of("Int")).hasToString("Int");
		assertThat(TypeVariable.of("Recursive")).hasToString("Recursive");
	}

	@Test
	void printsAGeneratedNameThatTheParserWillNotAccept() {
		// A name that is not an identifier is deliberately allowed: having no spelling the parser
		// accepts is what makes a generated name one no written signature can name. Such a variable
		// prints as-is and is not meant to be read back.
		TypeVariable generated = TypeVariable.of("Input#1");

		assertThat(generated).hasToString("Input#1");
		assertThatThrownBy(() -> Type.valueOf(generated.toString()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unexpected trailing input");
	}
}
