package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The types {@code EnvironmentBuilder} publishes for the variables it registers, and what type checking
 * makes of a reference to one.
 */
class EnvironmentVariableTypeTest {
	private static final Jackson2JsonProvider PROVIDER = Jackson2JsonProvider.getInstance();

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(PROVIDER, Versions.JQ_1_7);
	}

	private static CompileOptions strict() {
		return CompileOptions.newBuilder().setTypeCheckMode(TypeCheckMode.STRICT).build();
	}

	private static CompileOptions strict(List<Diagnostic> diagnostics) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setDiagnosticListener(diagnostics::add)
				.build();
	}

	/**
	 * The type of {@code $x} as the environment {@code configure} sets up publishes it.
	 */
	private static Type typeOfVariable(UnaryOperator<EnvironmentBuilder<JsonNode>> configure)
			throws JsonQueryException {
		return configure.apply(builder()).build().compile("$x", strict()).getType().outputType();
	}

	@Test
	void declaredVariableWithoutATypeStaysAny() throws JsonQueryException {
		Environment<JsonNode> environment = builder().declareVariable("n").build();
		assertThat(environment.compile("$n", strict()).getType().outputType()).isSameAs(AnyType.getInstance());
		// ANY accepts every use, so neither of these is a type problem.
		assertThatCode(() -> environment.compile("$n | floor", strict())).doesNotThrowAnyException();
		assertThatCode(() -> environment.compile("$n | .a", strict())).doesNotThrowAnyException();
	}

	@Test
	void declaredVariablePublishesItsType() throws JsonQueryException {
		Environment<JsonNode> environment = builder().declareVariable("n", NumericType.getInstance()).build();
		assertThat(environment.compile("$n", strict()).getType().outputType()).isSameAs(NumericType.getInstance());
		assertThat(environment.compile("$n | floor", strict()).getType().outputType()).isSameAs(NumericType.getInstance());
		assertThatThrownBy(() -> environment.compile("$n | .a", strict()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void definedVariablePublishesItsType() throws JsonQueryException {
		Environment<JsonNode> environment = builder()
				.defineVariable("s", StringType.getInstance(), () -> PROVIDER.createString("x"))
				.build();
		assertThat(environment.compile("$s | length", strict()).getType().outputType()).isSameAs(NumericType.getInstance());
		assertThatThrownBy(() -> environment.compile("$s | .a", strict()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void definedVariableWithoutATypeStaysAny() throws JsonQueryException {
		Environment<JsonNode> environment = builder()
				.defineVariable("s", () -> PROVIDER.createString("x"))
				.build();
		assertThat(environment.compile("$s", strict()).getType().outputType()).isSameAs(AnyType.getInstance());
	}

	@Test
	void constantTakesTheTypeReadOffItsValue() throws JsonQueryException {
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createString("a")))).isSameAs(StringType.getInstance());
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createBoolean(true)))).isSameAs(BooleanType.getInstance());
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createNull()))).isSameAs(NullType.getInstance());
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createBinary(new byte[] { 1, 2 }))))
				.isSameAs(BinaryType.getInstance());
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createNumber(1))))
				.isEqualTo(NumericType.of(NumberKind.INT));
		// The kind describes the value, not how the provider stores it: 1.0 is integral.
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createNumber(1.0))))
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createNumber(1.5))))
				.isEqualTo(NumericType.of(NumberKind.FLOAT));
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.createNumber(Double.NaN))))
				.isEqualTo(NumericType.of(NumberKind.FLOAT));
	}

	@Test
	void constantContainerTypesDescribeTheirContents() throws JsonQueryException {
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.parse("[1, \"a\"]"))))
				.isEqualTo(ArrayType.of(List.of(NumericType.of(NumberKind.INT), StringType.getInstance())));
		assertThat(typeOfVariable(b -> b.defineConstant("x", PROVIDER.parse("{\"a\": 1, \"b\": {\"c\": \"s\"}}"))))
				.isEqualTo(ObjectType.of(
						"a", NumericType.of(NumberKind.INT),
						"b", ObjectType.of("c", StringType.getInstance())));
	}

	@Test
	void anOverLongConstantArrayIsDescribedByTheUnionOfItsElements() throws JsonQueryException {
		// 32 leading positions are described one by one, as an array literal's are; past that the array is
		// open and every position has the same type.
		JsonNode thirtyTwo = PROVIDER.createArray(numbers(32));
		JsonNode thirtyThree = PROVIDER.createArray(numbers(33));
		assertThat(typeOfVariable(b -> b.defineConstant("x", thirtyTwo)))
				.isEqualTo(ArrayType.of(Collections.nCopies(32, NumericType.of(NumberKind.INT))));
		assertThat(typeOfVariable(b -> b.defineConstant("x", thirtyThree)))
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
	}

	@Test
	void anOverWideConstantObjectIsDescribedByTheUnionOfItsValues() throws JsonQueryException {
		Map<String, JsonNode> wide = new LinkedHashMap<>();
		for (int i = 0; i < 65; i++)
			wide.put("f" + i, PROVIDER.createNumber(i));
		JsonNode value = PROVIDER.createObject(wide);
		assertThat(typeOfVariable(b -> b.defineConstant("x", value)))
				.isEqualTo(ObjectType.of(UnionType.of(NumericType.of(NumberKind.INT))));
	}

	@Test
	void aDeeplyNestedConstantWidensToAnyPastTheDepthLimit() throws JsonQueryException {
		// 20 nested arrays around a string. The walk follows 17 of them -- depths 0 through 16 -- and
		// describes what is left as ANY rather than keeping going.
		JsonNode value = nestArrays(PROVIDER.createString("s"), 20);
		assertThat(typeOfVariable(b -> b.defineConstant("x", value)))
				.isEqualTo(nestArrayTypes(AnyType.getInstance(), 17));
	}

	@Test
	void aConstantObjectIsClosed() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		Environment<JsonNode> environment = builder()
				.defineConstant("cfg", PROVIDER.parse("{\"a\": 1}"))
				.build();
		assertThat(environment.compile("$cfg.typo", strict(diagnostics)).getType().outputType()).isSameAs(NullType.getInstance());
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).contains("Field \"typo\"").contains("closed object"));
	}

	@Test
	void anExplicitTypeReplacesTheOneReadOffTheValue() throws JsonQueryException {
		assertThat(typeOfVariable(b -> b.defineConstant("x", AnyType.getInstance(), PROVIDER.createString("a"))))
				.isSameAs(AnyType.getInstance());
		assertThat(typeOfVariable(b -> b.defineConstant("x", NumericType.getInstance(), PROVIDER.createString("a"))))
				.isSameAs(NumericType.getInstance());
	}

	@Test
	void theEnvironmentReportsEveryRegisteredVariablesType() {
		Environment<JsonNode> environment = builder()
				.declareVariable("declared", NumericType.getInstance())
				.defineVariable("defined", StringType.getInstance(), PROVIDER::createNull)
				.defineConstant("constant", PROVIDER.createString("a"))
				.declareVariable("untyped")
				.build();
		assertThat(environment.getDeclaredVariables()).isEqualTo(Map.of(
				"declared", NumericType.getInstance(),
				"untyped", AnyType.getInstance()));
		assertThat(environment.getVariables()).hasEntrySatisfying("defined",
				variable -> assertThat(variable.getType()).isSameAs(StringType.getInstance()));
		assertThat(environment.getConstants()).hasEntrySatisfying("constant",
				constant -> assertThat(constant.getType()).isSameAs(StringType.getInstance()));
	}

	@Test
	void aTypeChangesNothingWithCheckingOff() throws JsonQueryException {
		Environment<JsonNode> environment = builder().declareVariable("n", NumericType.getInstance()).build();
		// The declared type is wrong for this binding, and nothing notices: it is what type checking is
		// told, not a constraint on what may be supplied.
		JsonQuery<JsonNode> query = environment.compile("$n")
				.withRuntimeBindings(RuntimeBindings.<JsonNode>newBuilder()
						.setVariable("n", PROVIDER.createString("not a number"))
						.build());
		assertThat(query.getType().outputType()).isSameAs(AnyType.getInstance());
		assertThat(query.apply(PROVIDER.createNull())).containsExactly(PROVIDER.createString("not a number"));
	}

	private static JsonNode nestArrays(JsonNode value, int depth) {
		return depth == 0 ? value : nestArrays(PROVIDER.createArray(List.of(value)), depth - 1);
	}

	private static Type nestArrayTypes(Type type, int depth) {
		return depth == 0 ? type : nestArrayTypes(ArrayType.of(List.of(type)), depth - 1);
	}

	private static List<JsonNode> numbers(int count) {
		List<JsonNode> values = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			values.add(PROVIDER.createNumber(i));
		return values;
	}
}
