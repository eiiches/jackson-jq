package net.thisptr.jackson.jq.v2.ext.gzip;

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
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures the {@code jackson-jq/gzip} functions publish, seen both directly and through the
 * types {@code TypeCheck} infers for calls to them.
 */
class GzipTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/gzip\" as gzip; ";

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
	void compressionAnswersABinaryValue() throws JsonQueryException {
		assertThat(outputOf("gzip::compress_text", StringType.getInstance())).isSameAs(BinaryType.getInstance());
		assertThat(outputOf("gzip::compress_binary", BinaryType.getInstance())).isSameAs(BinaryType.getInstance());
	}

	@Test
	void onlyTheTextDecompressionAnswersAString() throws JsonQueryException {
		assertThat(outputOf("gzip::decompress_text", BinaryType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("gzip::decompress_binary", BinaryType.getInstance())).isSameAs(BinaryType.getInstance());
	}

	@Test
	void theCharsetOptionIsAnObject() throws JsonQueryException {
		assertThat(outputOf("gzip::compress_text({encoding: \"ISO-8859-1\"})", StringType.getInstance())).isSameAs(BinaryType.getInstance());
		assertThatThrownBy(() -> outputOf("gzip::compress_text(\"ISO-8859-1\")", StringType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void anUnknownOptionMemberIsRejected() {
		// The member value is read from the input, so the object is not constant-folded. A folded
		// constant publishes no type, which would leave nothing for the option type to be checked against.
		assertThatThrownBy(() -> outputOf("gzip::compress_text({misspelled: .})", StringType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void compressingTextRejectsANonStringInput() {
		assertThatThrownBy(() -> outputOf("gzip::compress_text", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}
}
