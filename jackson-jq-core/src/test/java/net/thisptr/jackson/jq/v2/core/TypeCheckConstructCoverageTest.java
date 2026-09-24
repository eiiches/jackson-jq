package net.thisptr.jackson.jq.v2.core;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One precisely inferred case and one strict-mode rejection for every executable construct.
 * <p>
 * Between them these say that no construct falls through to {@code ANY}: a construct with no rule of its
 * own could not produce the exact type the first half asks for, and could not reject the second half at
 * all. {@code TypeCheck} has no default branch, so a construct reaching it unhandled fails loudly rather
 * than quietly -- these rows are what make sure each one is reached.
 */
class TypeCheckConstructCoverageTest {
	private static final Type OBJ = ObjectType.of("a", NumericType.getInstance(), "b", StringType.getInstance());
	private static final Type INT = NumericType.of(NumberKind.INT);

	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
			.build();

	static Stream<Arguments> inferred() {
		return Stream.of(
				// identity, literals and composition
				Arguments.of(".", StringType.getInstance(), StringType.getInstance()),
				Arguments.of("1", NullType.getInstance(), INT),
				Arguments.of("\"x\"", NullType.getInstance(), StringType.of("x")),
				Arguments.of("[1, 2]", NullType.getInstance(), ArrayType.of(List.of(INT, INT))),
				// An array literal whose elements each emit one value knows what is at each position.
				Arguments.of("[1, \"a\"] | .[0] - 1", NullType.getInstance(), NumericType.getInstance()),
				Arguments.of("[1, \"a\"] | .[-1]", NullType.getInstance(), StringType.of("a")),
				Arguments.of("[1, 2] | .[5]", NullType.getInstance(), NullType.getInstance()),
				Arguments.of("[.[]] | .[0]", ArrayType.of(INT), UnionType.of(INT, NullType.getInstance())),
				Arguments.of("[1] + [\"a\"] | .[1]", NullType.getInstance(), StringType.of("a")),
				Arguments.of("[[1, 2]][] as [$i, $j] | $i * $j", NullType.getInstance(), NumericType.getInstance()),
				// Concatenating onto an array of unknown length says nothing about where anything landed.
				Arguments.of("[.[]] + [\"a\"] | .[1]", ArrayType.of(INT), UnionType.of(INT, StringType.of("a"), NullType.getInstance())),
				// A slice and a subtraction both move elements, so neither keeps a position.
				Arguments.of("[1, \"a\"] | .[0:1] | .[0]", NullType.getInstance(), UnionType.of(INT, StringType.of("a"), NullType.getInstance())),
				Arguments.of("[1, \"a\"] - [1] | .[0]", NullType.getInstance(), UnionType.of(INT, StringType.of("a"), NullType.getInstance())),
				Arguments.of("[]", NullType.getInstance(), ArrayType.of(NeverType.getInstance())),
				Arguments.of("{a: 1}", NullType.getInstance(), ObjectType.of("a", INT)),
				Arguments.of(". | length", StringType.getInstance(), NumericType.getInstance()),
				Arguments.of("(1, \"x\")", NullType.getInstance(), UnionType.of(INT, StringType.of("x"))),
				Arguments.of("\"v=\\(1)\"", NullType.getInstance(), StringType.getInstance()),
				// field and index access
				Arguments.of(".a", OBJ, NumericType.getInstance()),
				Arguments.of(".\"a\"", OBJ, NumericType.getInstance()),
				Arguments.of(".[\"a\"]", OBJ, NumericType.getInstance()),
				Arguments.of(".[0]", ArrayType.of(StringType.getInstance()), UnionType.of(StringType.getInstance(), NullType.getInstance())),
				Arguments.of(".[1:]", ArrayType.of(StringType.getInstance()), ArrayType.of(StringType.getInstance())),
				Arguments.of(".[]", OBJ, UnionType.of(NumericType.getInstance(), StringType.getInstance())),
				Arguments.of(".[[1]]", ArrayType.of(NumericType.getInstance()), ArrayType.of(NumericType.getInstance())),
				Arguments.of(".a?", OBJ, NumericType.getInstance()),
				// operators
				Arguments.of("1 + 2", NullType.getInstance(), NumericType.getInstance()),
				Arguments.of("[1] + [2]", NullType.getInstance(), ArrayType.of(List.of(INT, INT))),
				Arguments.of("{a: 1} + {b: 2}", NullType.getInstance(), ObjectType.of("a", INT, "b", INT)),
				Arguments.of("[1, 2] - [2]", NullType.getInstance(), ArrayType.of(INT)),
				Arguments.of("\"x\" * 2", NullType.getInstance(), UnionType.of(StringType.getInstance(), NullType.getInstance())),
				Arguments.of("{a: {b: 1}} * {a: {c: 2}}", NullType.getInstance(),
						ObjectType.of("a", ObjectType.of("b", INT, "c", INT))),
				Arguments.of("\"a,b\" / \",\"", NullType.getInstance(), ArrayType.of(StringType.getInstance())),
				Arguments.of("5 % 2", NullType.getInstance(), NumericType.getInstance()),
				Arguments.of("-.", NumericType.getInstance(), NumericType.getInstance()),
				Arguments.of("1 < 2", NullType.getInstance(), BooleanType.getInstance()),
				Arguments.of("true and false", NullType.getInstance(), BooleanType.of(false)),
				Arguments.of(".a // \"fallback\"", ObjectType.of("a", UnionType.of(StringType.getInstance(), NullType.getInstance())), StringType.getInstance()),
				// control flow
				Arguments.of("if type == \"number\" then . - 1 else 0 end", UnionType.of(NumericType.getInstance(), StringType.getInstance()), NumericType.getInstance()),
				Arguments.of("try .a catch \"e\"", OBJ, UnionType.of(NumericType.getInstance(), StringType.of("e"))),
				Arguments.of("label $out | (1, break $out)", NullType.getInstance(), INT),
				Arguments.of("..", ObjectType.of("a", NumericType.getInstance()),
						UnionType.of(ObjectType.of("a", NumericType.getInstance()), NumericType.getInstance())),
				// binding and destructuring
				Arguments.of(". as $x | $x", StringType.getInstance(), StringType.getInstance()),
				Arguments.of(". as {a: $x} | $x", OBJ, NumericType.getInstance()),
				// An array of unknown length may not reach position 0, and `[] as [$x]` really does bind null.
				Arguments.of(". as [$x] | $x", ArrayType.of(StringType.getInstance()), UnionType.of(StringType.getInstance(), NullType.getInstance())),
				// loops
				Arguments.of("reduce .[] as $x (0; . + $x)", ArrayType.of(NumericType.getInstance()), NumericType.getInstance()),
				Arguments.of("foreach .[] as $x ([]; . + [$x]; .)", ArrayType.of(StringType.getInstance()), ArrayType.of(StringType.getInstance())),
				// definitions and calls
				Arguments.of("def f: .a; f", OBJ, NumericType.getInstance()),
				Arguments.of("def m(g): [.[] | g]; m(tostring)", ArrayType.of(NumericType.getInstance()), ArrayType.of(StringType.getInstance())),
				Arguments.of("def f($n): $n + 1; f(1)", NullType.getInstance(), NumericType.getInstance()),
				Arguments.of("tostring", NumericType.getInstance(), StringType.getInstance()),
				Arguments.of("map(tostring)", ArrayType.of(NumericType.getInstance()), ArrayType.of(StringType.getInstance())),
				// assignment
				Arguments.of(".a = \"x\"", OBJ, ObjectType.of("a", StringType.of("x"), "b", StringType.getInstance())),
				Arguments.of(".a |= tostring", OBJ, ObjectType.of("a", StringType.getInstance(), "b", StringType.getInstance())),
				Arguments.of(".a += 1", OBJ, ObjectType.of("a", NumericType.getInstance(), "b", StringType.getInstance())),
				Arguments.of(".[] |= tostring", ArrayType.of(NumericType.getInstance()), ArrayType.of(StringType.getInstance())),
				Arguments.of(".[0:1] = [\"x\"]", ArrayType.of(NumericType.getInstance()), ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.of("x")))));
	}

	static Stream<Arguments> rejected() {
		return Stream.of(
				// A builtin's body is the caller's to hear about now, and `map` is `[.[] | f]`.
				Arguments.of("map(.)", NumericType.getInstance()),
				// An array pattern against something no array pattern can match is still an error.
				Arguments.of(". as [$x] | $x", NumericType.getInstance()),
				Arguments.of("[1, 2] as [$a, {c: $b}] | $b", NullType.getInstance()),
				// The second position of a two-element literal is a string, whatever the first one is.
				Arguments.of("[1, \"a\"] | .[1] - 1", NullType.getInstance()),
				Arguments.of(".a", NumericType.getInstance()),
				Arguments.of(".\"a\"", NumericType.getInstance()),
				Arguments.of(".[\"a\"]", ArrayType.of(StringType.getInstance())),
				Arguments.of(".[0]", StringType.getInstance()),
				Arguments.of(".[1:]", NumericType.getInstance()),
				Arguments.of(".[true:]", ArrayType.of(StringType.getInstance())),
				Arguments.of(".[]", NumericType.getInstance()),
				Arguments.of(".[[1]]", StringType.getInstance()),
				Arguments.of("{(1): 2}", NullType.getInstance()),
				Arguments.of("{a: 1} + 1", NullType.getInstance()),
				Arguments.of("[1] - 1", NullType.getInstance()),
				Arguments.of("true * 2", NullType.getInstance()),
				Arguments.of("[1] / 2", NullType.getInstance()),
				Arguments.of("\"x\" % 2", NullType.getInstance()),
				Arguments.of("-.", StringType.getInstance()),
				Arguments.of("\"x\" | if . then .a else . end", StringType.getInstance()),
				Arguments.of("[1, 2] | .. | .a", NullType.getInstance()),
				Arguments.of("1 as [$x] | $x", NullType.getInstance()),
				Arguments.of("1 as {a: $x} | $x", NullType.getInstance()),
				Arguments.of("reduce .[] as $x (0; .a)", ArrayType.of(NumericType.getInstance())),
				Arguments.of("foreach .[] as $x (0; .a; .)", ArrayType.of(NumericType.getInstance())),
				Arguments.of("def f: . + 1; f", StringType.getInstance()),
				Arguments.of("def m(g): [.[] | g]; m(.)", NumericType.getInstance()),
				Arguments.of("map(.a)", ArrayType.of(NumericType.getInstance())),
				Arguments.of("tostring | . + 1", NumericType.getInstance()),
				Arguments.of(". = .a", NumericType.getInstance()),
				Arguments.of(".a |= empty", ObjectType.of(AnyType.getInstance())),
				Arguments.of(".a += \"x\"", OBJ),
				Arguments.of(".[0:1] = \"x\"", ArrayType.of(NumericType.getInstance())),
				Arguments.of("(.a, .b)", NumericType.getInstance()),
				Arguments.of("foreach .[] as $x (0; .; .a)", ArrayType.of(NumericType.getInstance())),
				Arguments.of("\"prefix \\(.a)\"", NumericType.getInstance()));
	}

	@ParameterizedTest(name = "{0} on {1}")
	@MethodSource("inferred")
	void infersExactly(String query, Type input, Type expected) throws JsonQueryException {
		assertThat(environment.compile(query, strict(input)).getType().outputType()).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0} on {1}")
	@MethodSource("rejected")
	void rejectsInStrictMode(String query, Type input) {
		assertThatThrownBy(() -> environment.compile(query, strict(input)))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}
}
