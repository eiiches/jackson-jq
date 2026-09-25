package net.thisptr.jackson.jq.v2.ext.http;

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
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures the {@code jackson-jq/http} functions publish, seen both directly and through the
 * types {@code TypeCheck} infers for calls to them.
 */
class HttpTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/http\" as http; ";

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
	void theResponseCarriesTheStatusHeadersAndBody() throws JsonQueryException {
		Type output = outputOf("http::get(\"https://example.com\")", AnyType.getInstance());
		assertThat(output).isInstanceOfSatisfying(ObjectType.class,
				response -> assertThat(response.fields()).containsKeys("body", "headers", "raw_body", "status"));
		assertThat(outputOf("http::get(\"https://example.com\") | .status", AnyType.getInstance()).toString()).isEqualTo("INT");
		assertThat(outputOf("http::get(\"https://example.com\") | .raw_body", AnyType.getInstance())).isSameAs(BinaryType.getInstance());
		assertThat(outputOf("http::get(\"https://example.com\") | .headers[].name", AnyType.getInstance())).isSameAs(StringType.getInstance());
	}

	@Test
	void theUrlMustBeAString() {
		assertThatThrownBy(() -> outputOf("http::get(42)", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void theOptionsOverloadKeepsTheSameResponse() throws JsonQueryException {
		assertThat(outputOf("http::get(\"https://example.com\"; {timeout: 5}) | .status", AnyType.getInstance()).toString())
				.isEqualTo("INT");
	}
}
