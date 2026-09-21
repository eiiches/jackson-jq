package net.thisptr.jackson.jq.v2.ext.fs;

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
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures the {@code jackson-jq/fs} functions publish, seen both directly and through the
 * types {@code TypeCheck} infers for calls to them.
 */
class FsTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/fs\" as fs; ";

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
	void readingAnswersWhatWasAskedFor() throws JsonQueryException {
		assertThat(outputOf("fs::read_text(\"f\")", AnyType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("fs::read_binary(\"f\")", AnyType.getInstance())).isSameAs(BinaryType.getInstance());
		assertThat(outputOf("fs::read_json(\"f\")", AnyType.getInstance())).isSameAs(AnyType.getInstance());
	}

	@Test
	void everyWriteAnswersNull() throws JsonQueryException {
		assertThat(outputOf("fs::write_text(\"f\")", StringType.getInstance())).isSameAs(NullType.getInstance());
		assertThat(outputOf("fs::write_binary(\"f\")", BinaryType.getInstance())).isSameAs(NullType.getInstance());
		assertThat(outputOf("fs::write_json(\"f\")", AnyType.getInstance())).isSameAs(NullType.getInstance());
	}

	@Test
	void listingAnswersPathAndTypeEntries() throws JsonQueryException {
		assertThat(outputOf("fs::list(\"d\")", AnyType.getInstance()).toString()).isEqualTo("[*:{path:STRING,type:STRING}]");
		assertThat(outputOf("fs::list(\"d\") | .[].path", AnyType.getInstance())).isSameAs(StringType.getInstance());
	}

	@Test
	void theInputFlowsThroughToTheArguments() throws JsonQueryException {
		assertThat(outputOf("fs::read_text(.)", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> outputOf("fs::read_text(.)", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void optionsMustBeAnObject() throws JsonQueryException {
		assertThat(outputOf("fs::read_text(\"f\"; {encoding: \"UTF-8\"})", AnyType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("fs::list(\"d\"; {\"recursive\": true})", AnyType.getInstance()).toString())
				.isEqualTo("[*:{path:STRING,type:STRING}]");
		assertThatThrownBy(() -> outputOf("fs::read_text(\"f\"; \"UTF-8\")", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void anUnknownOptionMemberIsRejected() throws JsonQueryException {
		assertThat(outputOf("fs::read_text(\"f\"; {encoding: \"UTF-8\"})", AnyType.getInstance())).isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> outputOf("fs::read_text(\"f\"; {misspelled: \"UTF-8\"})", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("fs::list(\"d\"; {recursive: true, typo: true})", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("fs::list(\"d\"; {\"recursive\": true, \"typo\": true})", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void anOptionMemberOfTheWrongTypeIsRejected() {
		assertThatThrownBy(() -> outputOf("fs::read_text(\"f\"; {encoding: 42})", AnyType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void writingTextRejectsANonStringInput() {
		assertThatThrownBy(() -> outputOf("fs::write_text(\"f\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}
}
