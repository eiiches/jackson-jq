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
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures published by the regex primitives the jq-source {@code match}/{@code sub} surface is
 * built on. The jq-level definitions themselves are not covered here: a jq {@code def} is its own
 * inference boundary, so a call to one types as {@code ANY} whatever its body does.
 */
class Re2TypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/re2/_impl\" as re2_impl; ";

	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7).build();

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private Type outputOf(String query, Type inputType) throws JsonQueryException {
		return environment.compile(IMPORT + query, strict(inputType)).getType().outputType();
	}

	@Test
	void everyRegisteredSignatureDeclaresSchemesForItsArity() {
		new InternalModuleImpl().getFunctions().forEach((signature, function) -> {
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
		new InternalModuleImpl().getFunctions().forEach((signature, function) -> {
			for (TypeScheme<FunctionType> scheme : function.types(Versions.JQ_1_7, 3))
				assertThat(scheme.body().returnType().inputType()).describedAs("%s", signature).isSameAs(StringType.getInstance());
		});
	}

	@Test
	void testModeAndMatchModeShareOneSignature() throws JsonQueryException {
		Type output = outputOf("re2_impl::_match_impl(\"a\"; null; true)", StringType.getInstance());
		assertThat(output.toString()).contains("BOOLEAN").contains("captures");
	}

	@Test
	void substitutionAnswersAString() throws JsonQueryException {
		assertThat(outputOf("re2_impl::_sub_impl(\"a\"; \"b\"; \"g\")", StringType.getInstance())).isSameAs(StringType.getInstance());
	}

	@Test
	void aNonStringInputIsRejected() {
		assertThatThrownBy(() -> outputOf("re2_impl::_sub_impl(\"a\"; \"b\"; \"g\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}
}
