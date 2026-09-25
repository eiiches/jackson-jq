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
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures published by the regex primitives the jq-source {@code match}/{@code sub} surface
 * is built on.
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

	@Test
	void testModeAndMatchModeShareOneSignature() throws JsonQueryException {
		Type output = environment.compile("_match_impl(\"a\"; null; true)", strict(StringType.getInstance()))
				.getType().outputType();
		assertThat(output.toString()).contains("BOOLEAN").contains("captures");
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
}
