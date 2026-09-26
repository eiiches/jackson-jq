package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
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
 * surface built on them.
 */
class RegexTypeSchemeTest {
	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7).build();

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private Type outputOf(String query) throws JsonQueryException {
		return environment.compile(query, strict(StringType.getInstance())).getType().outputType();
	}

	@Test
	void bothPrimitivesDeclareThreeParameters() {
		for (Function function : List.of(new _MatchImplFunction(), new _SubImplFunction())) {
			List<TypeScheme<FunctionType>> schemes = function.types(Versions.JQ_1_7, 3);
			assertThat(schemes).describedAs(function.getClass().getSimpleName()).isNotEmpty();
			for (TypeScheme<FunctionType> scheme : schemes) {
				assertThat(scheme.body().parameterTypes()).hasSize(3);
				assertThat(scheme.body().returnType().inputType()).isSameAs(StringType.getInstance());
			}
		}
	}

	/**
	 * The primitive's last argument decides which of its two signatures a call has: the test mode answers
	 * whether the regex matched, the match mode answers the matches themselves.
	 */
	@Test
	void theTestModeArgumentPicksTheSignature() throws JsonQueryException {
		assertThat(outputOf("_match_impl(\"a\"; \"\"; true)")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("_match_impl(\"a\"; \"\"; false)").toString()).startsWith("[*:{").contains("captures");
	}

	@Test
	void substitutionAnswersAString() throws JsonQueryException {
		assertThat(environment.compile("_sub_impl(\"a\"; \"b\"; \"g\")", strict(StringType.getInstance()))
				.getType().outputType()).isSameAs(StringType.getInstance());
	}

	@Test
	void aNonStringInputIsRejected() {
		assertThatThrownBy(() -> environment.compile("_sub_impl(\"a\"; \"b\"; \"g\")", strict(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void aMatchIsAnObjectWhoseOwnStringIsNeverNull() throws JsonQueryException {
		String match = "{captures:[*:{length:INT,name:NULL|STRING,offset:INT,string:NULL|STRING}],length:INT,offset:INT,string:STRING}";
		assertThat(outputOf("match(\"a\"; \"\")")).hasToString(match);
		assertThat(outputOf("match(\"a\")")).hasToString(match);
		assertThat(outputOf("[match(\"a\"; \"g\")]")).hasToString("[*:" + match + "]");
	}

	/**
	 * The manual gives the one-argument form two spellings, {@code FILTER([REGEX])} and
	 * {@code FILTER([REGEX, FLAGS])}, and those are the two this accepts.
	 */
	@Test
	void aPatternAndItsFlagsMayArriveInOneArray() throws JsonQueryException {
		assertThat(outputOf("test([\"a\"])")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("test([\"a\", \"g\"])")).isSameAs(BooleanType.getInstance());
	}

	@Test
	void aPatternThatIsNeitherAStringNorANonEmptyArrayIsRejected() {
		for (String query : List.of("match(123)", "test([])", "capture({})")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void testAnswersABooleanRatherThanThePrimitivesUnion() throws JsonQueryException {
		assertThat(outputOf("test(\"a\"; \"\")")).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("[test(\"a\"; \"g\")]")).hasToString("[*:BOOLEAN]");
	}

	@Test
	void captureIsKeyedByNamesOnlyKnownAtRuntime() throws JsonQueryException {
		assertThat(outputOf("capture(\"a\"; \"\")")).hasToString("{*:NULL|STRING}");
		assertThat(outputOf("capture(\"a\")")).hasToString("{*:NULL|STRING}");
	}

	@Test
	void scanAnswersTheCapturesWhenThereAreAnyAndTheMatchWhenThereAreNot() throws JsonQueryException {
		assertThat(outputOf("scan(\"a\")")).hasToString("STRING|[*:NULL|STRING]");
		assertThat(outputOf("scan(\"a\"; \"g\")")).hasToString("STRING|[*:NULL|STRING]");
	}

	/**
	 * The manual asks for string flags throughout, and every one-argument form here reaches its
	 * two-argument counterpart with {@code ""}, so nothing on this surface has a reason to take a null.
	 */
	@Test
	void noFlagsArgumentOnTheSurfaceTakesANull() {
		for (String query : List.of("match(\"a\"; null)", "test(\"a\"; null)", "capture(\"a\"; null)",
				"scan(\"a\"; null)", "splits(\"a\"; null)", "split(\"a\"; null)",
				"match([\"a\", null])", "match([\"a\", \"g\", 1])", "_match_impl(\"a\"; null; false)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void splittingAnswersStrings() throws JsonQueryException {
		assertThat(outputOf("splits(\"a\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("splits(\"a\"; \"g\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf("split(\"a\"; \"g\")")).hasToString("[*:STRING]");
	}

	@Test
	void theSubstitutionSurfaceAnswersAString() throws JsonQueryException {
		for (String query : List.of("sub(\"a\"; \"b\")", "sub(\"a\"; \"b\"; \"g\")",
				"gsub(\"a\"; \"b\")", "gsub(\"a\"; \"b\"; \"g\")")) {
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
		for (String query : List.of("sub(\"a\"; null)", "gsub(\"a\"; null)",
				"sub(\"a\"; \"b\"; null)", "gsub(\"a\"; \"b\"; null)")) {
			assertThatThrownBy(() -> outputOf(query))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}
}
