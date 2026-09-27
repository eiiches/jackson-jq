package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures published by the regex primitives, and by the jq-source {@code match}/{@code sub}
 * surface built on them -- which states its own with {@code #jackson-jq:type} comments, since a
 * {@code def} left to its body would report the primitive's whole union or, where it dispatches on
 * {@code $val|type}, nothing at all.
 * <p>
 * The surface is reached qualified, as an importer reaches it. The same assertions hold unqualified
 * against the joni engine, whose {@code JqFunction}s state the same signatures.
 */
class Re2TypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/re2\" as re2; ";

	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
			.clearModuleLoaders()
			.registerModule(Re2RegexModule.getInstance())
			.build();

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private Type outputOf(String query, Type inputType) throws JsonQueryException {
		return environment.compile(IMPORT + query, strict(inputType)).getType().outputType();
	}

	private Type outputOf(String query) throws JsonQueryException {
		return outputOf(query, StringType.getInstance());
	}

	@Test
	void everyRegisteredSignatureDeclaresSchemesForItsArity() {
		Re2RegexModule.getInstance().getFunctions().forEach((signature, function) -> {
			// No module function is variadic, so every registration names a concrete arity.
			int arity = Objects.requireNonNull(signature.arity(), "arity");
			List<TypeScheme<FunctionType>> schemes = function.types(Versions.JQ_1_7, arity);
			assertThat(schemes).describedAs("%s", signature).isNotEmpty();
			for (TypeScheme<FunctionType> scheme : schemes)
				assertThat(scheme.body().parameterTypes()).describedAs("%s", signature).hasSize(arity);
		});
	}

	@Test
	void bothPrimitivesTakeAStringInput() {
		Re2RegexModule.getInstance().getFunctions().forEach((signature, function) -> {
			for (TypeScheme<FunctionType> scheme : function.types(Versions.JQ_1_7, 3))
				assertThat(scheme.body().returnType().inputType()).describedAs("%s", signature).isSameAs(StringType.getInstance());
		});
	}

	/**
	 * The primitive's last argument decides which of its two signatures a call has: the test mode answers
	 * whether the regex matched, the match mode answers the matches themselves.
	 */
	@Test
	void theTestModeArgumentPicksTheSignature() throws JsonQueryException {
		assertThat(outputOf("re2::_match_impl(\"a\"; \"\"; true)", StringType.getInstance()))
				.isSameAs(BooleanType.getInstance());
		assertThat(outputOf("re2::_match_impl(\"a\"; \"\"; false)", StringType.getInstance()).toString())
				.startsWith("[*:{").contains("captures");
	}

	@Test
	void substitutionAnswersAString() throws JsonQueryException {
		assertThat(outputOf("re2::_sub_impl(\"a\"; \"b\"; \"g\")", StringType.getInstance())).isSameAs(StringType.getInstance());
	}

	@Test
	void aMatchIsAnObjectWhoseOwnStringIsNeverNull() throws JsonQueryException {
		String match = "{captures:[*:{length:INT,name:NULL|STRING,offset:INT,string:NULL|STRING}],length:INT,offset:INT,string:STRING}";
		assertThat(outputOf("re2::match(\"a\"; \"\")")).hasToString(match);
		assertThat(outputOf("re2::match(\"a\")")).hasToString(match);
		assertThat(outputOf("[re2::match(\"a\"; \"g\")]")).hasToString("[*:" + match + "]");
	}

	/**
	 * The manual gives the one-argument form two spellings, {@code FILTER([REGEX])} and
	 * {@code FILTER([REGEX, FLAGS])}, and those are the two this accepts.
	 */
	@Test
	void aPatternAndItsFlagsMayArriveInOneArray() throws JsonQueryException {
		assertThat(outputOf("re2::test([\"a\"])")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("re2::test([\"a\", \"g\"])")).isSameAs(BooleanType.getInstance());
	}

	@Test
	void aPatternThatIsNeitherAStringNorANonEmptyArrayIsRejected() {
		for (String query : List.of("re2::match(123)", "re2::test([])", "re2::capture({})")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void testAnswersABooleanRatherThanThePrimitivesUnion() throws JsonQueryException {
		assertThat(outputOf("re2::test(\"a\"; \"\")")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("[re2::test(\"a\"; \"g\")]")).hasToString("[*:BOOLEAN]");
	}

	@Test
	void captureIsKeyedByNamesOnlyKnownAtRuntime() throws JsonQueryException {
		assertThat(outputOf("re2::capture(\"a\"; \"\")")).hasToString("{*:NULL|STRING}");
		assertThat(outputOf("re2::capture(\"a\")")).hasToString("{*:NULL|STRING}");
	}

	@Test
	void scanAnswersTheCapturesWhenThereAreAnyAndTheMatchWhenThereAreNot() throws JsonQueryException {
		assertThat(outputOf("re2::scan(\"a\")")).hasToString("STRING|[*:NULL|STRING]");
		assertThat(outputOf("re2::scan(\"a\"; \"g\")")).hasToString("STRING|[*:NULL|STRING]");
	}

	/**
	 * The manual asks for string flags throughout, and every one-argument form here reaches its
	 * two-argument counterpart with {@code ""}, so nothing on this surface has a reason to take a null.
	 */
	@Test
	void noFlagsArgumentOnTheSurfaceTakesANull() {
		for (String query : List.of("re2::match(\"a\"; null)", "re2::test(\"a\"; null)", "re2::capture(\"a\"; null)",
				"re2::scan(\"a\"; null)", "re2::splits(\"a\"; null)", "re2::split(\"a\"; null)",
				"re2::match([\"a\", null])", "re2::match([\"a\", \"g\", 1])", "re2::_match_impl(\"a\"; null; false)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void splittingAnswersStrings() throws JsonQueryException {
		assertThat(outputOf("re2::splits(\"a\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("re2::splits(\"a\"; \"g\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("re2::split(\"a\"; \"g\")")).hasToString("[*:STRING]");
	}

	@Test
	void theSubstitutionSurfaceAnswersAString() throws JsonQueryException {
		for (String query : List.of("re2::sub(\"a\"; \"b\")", "re2::sub(\"a\"; \"b\"; \"g\")",
				"re2::gsub(\"a\"; \"b\")", "re2::gsub(\"a\"; \"b\"; \"g\")")) {
			assertThat(outputOf(query)).describedAs(query).isSameAs(StringType.getInstance());
		}
	}

	/**
	 * A null replacement contributes nothing instead of failing, and {@code gsub}'s {@code flags + "g"}
	 * absorbs a null, but both are accidents of how the definitions are written rather than signatures to
	 * publish.
	 */
	@Test
	void theSubstitutionSurfaceTakesNeitherANullReplacementNorNullFlags() {
		for (String query : List.of("re2::sub(\"a\"; null)", "re2::gsub(\"a\"; null)",
				"re2::sub(\"a\"; \"b\"; null)", "re2::gsub(\"a\"; \"b\"; null)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void aNonStringInputIsRejected() {
		assertThatThrownBy(() -> outputOf("re2::_sub_impl(\"a\"; \"b\"; \"g\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}
}
