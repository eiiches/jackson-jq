package net.thisptr.jackson.jq.v2.ext.uri;

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
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures the {@code jackson-jq/uri} functions publish, seen both directly and through the
 * types {@code TypeCheck} infers for calls to them.
 */
class UriTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/uri\" as uri; ";

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
		new ModuleImpl().getFunctions().forEach((signature, function) -> {
			// No module function is variadic, so every registration names a concrete arity.
			int arity = Objects.requireNonNull(signature.arity(), "arity");
			List<TypeScheme<FunctionType>> schemes = function.types(Versions.JQ_1_7, arity);
			assertThat(schemes).describedAs("%s", signature).isNotEmpty();
			for (TypeScheme<FunctionType> scheme : schemes)
				assertThat(scheme.body().parameterTypes()).describedAs("%s", signature).hasSize(arity);
		});
	}

	@Test
	void decodingAnswersAString() throws JsonQueryException {
		assertThat(outputOf("uri::uridecode", StringType.getInstance())).isSameAs(StringType.getInstance());
	}

	@Test
	void parsingAnswersTheComponentsOfTheUri() throws JsonQueryException {
		Type output = outputOf("uri::uriparse", StringType.getInstance());
		assertThat(output).isInstanceOfSatisfying(ObjectType.class,
				components -> assertThat(components.fields()).containsKeys("host", "query_obj", "raw_fragment"));
		assertThat(outputOf("uri::uriparse | .host", StringType.getInstance()).toString()).isEqualTo("NULL|STRING");
		assertThat(outputOf("uri::uriparse | .port", StringType.getInstance()).toString()).isEqualTo("INT");
	}

	@Test
	void aNonStringInputIsRejected() {
		assertThatThrownBy(() -> outputOf("uri::uridecode", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}
}
