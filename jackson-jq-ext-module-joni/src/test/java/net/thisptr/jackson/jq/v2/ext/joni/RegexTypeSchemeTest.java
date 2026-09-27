package net.thisptr.jackson.jq.v2.ext.joni;

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
 * The surface is reached qualified, as an importer reaches it. The same assertions hold against the
 * re2 engine, whose module states the same signatures.
 */
class RegexTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/joni\" as joni; ";

	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7).registerModule(new JoniRegexModule())
			.clearModuleLoaders()
			.registerModule(new JoniRegexModule())
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
		new JoniRegexModule().getFunctions().forEach((signature, function) -> {
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
		new JoniRegexModule().getFunctions().forEach((signature, function) -> {
			for (TypeScheme<FunctionType> scheme : function.types(Versions.JQ_1_7, 3))
				assertThat(scheme.body().returnType().inputType()).describedAs("%s", signature)
						.isSameAs(StringType.getInstance());
		});
	}

	/**
	 * The primitive's last argument decides which of its two signatures a call has: the test mode answers
	 * whether the regex matched, the match mode answers the matches themselves.
	 */
	@Test
	void theTestModeArgumentPicksTheSignature() throws JsonQueryException {
		assertThat(outputOf("joni::_match_impl(\"a\"; \"\"; true)")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("joni::_match_impl(\"a\"; \"\"; false)").toString()).startsWith("[*:{").contains("captures");
	}

	@Test
	void substitutionAnswersAString() throws JsonQueryException {
		assertThat(outputOf("joni::_sub_impl(\"a\"; \"b\"; \"g\")")).isSameAs(StringType.getInstance());
	}

	@Test
	void aNonStringInputIsRejected() {
		assertThatThrownBy(() -> outputOf("joni::_sub_impl(\"a\"; \"b\"; \"g\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void aMatchIsAnObjectWhoseOwnStringIsNeverNull() throws JsonQueryException {
		String match = "{captures:[*:{length:INT,name:NULL|STRING,offset:INT,string:NULL|STRING}],length:INT,offset:INT,string:STRING}";
		assertThat(outputOf("joni::match(\"a\"; \"\")")).hasToString(match);
		assertThat(outputOf("joni::match(\"a\")")).hasToString(match);
		assertThat(outputOf("[joni::match(\"a\"; \"g\")]")).hasToString("[*:" + match + "]");
	}

	/**
	 * The manual gives the one-argument form two spellings, {@code FILTER([REGEX])} and
	 * {@code FILTER([REGEX, FLAGS])}, and those are the two this accepts.
	 */
	@Test
	void aPatternAndItsFlagsMayArriveInOneArray() throws JsonQueryException {
		assertThat(outputOf("joni::test([\"a\"])")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("joni::test([\"a\", \"g\"])")).isSameAs(BooleanType.getInstance());
	}

	@Test
	void aPatternThatIsNeitherAStringNorANonEmptyArrayIsRejected() {
		for (String query : List.of("joni::match(123)", "joni::test([])", "joni::capture({})")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void testAnswersABooleanRatherThanThePrimitivesUnion() throws JsonQueryException {
		assertThat(outputOf("joni::test(\"a\"; \"\")")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("[joni::test(\"a\"; \"g\")]")).hasToString("[*:BOOLEAN]");
	}

	@Test
	void captureIsKeyedByNamesOnlyKnownAtRuntime() throws JsonQueryException {
		assertThat(outputOf("joni::capture(\"a\"; \"\")")).hasToString("{*:NULL|STRING}");
		assertThat(outputOf("joni::capture(\"a\")")).hasToString("{*:NULL|STRING}");
	}

	@Test
	void scanAnswersTheCapturesWhenThereAreAnyAndTheMatchWhenThereAreNot() throws JsonQueryException {
		assertThat(outputOf("joni::scan(\"a\")")).hasToString("STRING|[*:NULL|STRING]");
		assertThat(outputOf("joni::scan(\"a\"; \"g\")")).hasToString("STRING|[*:NULL|STRING]");
	}

	/**
	 * The manual asks for string flags throughout, and every one-argument form here reaches its
	 * two-argument counterpart with {@code ""}, so nothing on this surface has a reason to take a null.
	 */
	@Test
	void noFlagsArgumentOnTheSurfaceTakesANull() {
		for (String query : List.of("joni::match(\"a\"; null)", "joni::test(\"a\"; null)", "joni::capture(\"a\"; null)",
				"joni::scan(\"a\"; null)", "joni::splits(\"a\"; null)", "joni::split(\"a\"; null)",
				"joni::match([\"a\", null])", "joni::match([\"a\", \"g\", 1])", "joni::_match_impl(\"a\"; null; false)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void splittingAnswersStrings() throws JsonQueryException {
		assertThat(outputOf("joni::splits(\"a\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("joni::splits(\"a\"; \"g\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("joni::split(\"a\"; \"g\")")).hasToString("[*:STRING]");
	}

	@Test
	void theSubstitutionSurfaceAnswersAString() throws JsonQueryException {
		for (String query : List.of("joni::sub(\"a\"; \"b\")", "joni::sub(\"a\"; \"b\"; \"g\")",
				"joni::gsub(\"a\"; \"b\")", "joni::gsub(\"a\"; \"b\"; \"g\")")) {
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
		for (String query : List.of("joni::sub(\"a\"; null)", "joni::gsub(\"a\"; null)",
				"joni::sub(\"a\"; \"b\"; null)", "joni::gsub(\"a\"; \"b\"; null)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}
}
