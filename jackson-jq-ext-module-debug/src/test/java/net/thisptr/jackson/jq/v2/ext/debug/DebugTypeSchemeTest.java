package net.thisptr.jackson.jq.v2.ext.debug;

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
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The signatures the {@code jackson-jq/debug} functions publish, seen both directly and through the
 * types {@code TypeCheck} infers for calls to them.
 */
class DebugTypeSchemeTest {
	private static final String IMPORT = "import \"jackson-jq/debug\" as debug; ";

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
	void theScopeEchoesTheInputType() throws JsonQueryException {
		assertThat(outputOf("debug::debug_scope | .input", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("debug::debug_scope | .scope.functions", AnyType.getInstance()).toString()).isEqualTo("{*:ANY}");
	}

	@Test
	void expressionIntrospectionAnswersBooleans() throws JsonQueryException {
		assertThat(outputOf("debug::debug_expr(.) | .depends_on_input", AnyType.getInstance())).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("debug::dump_expr(.)", AnyType.getInstance()).toString()).isEqualTo("{*:ANY}");
	}

	@Test
	void theArgumentIsNeverEvaluatedSoItConstrainsNothing() throws JsonQueryException {
		assertThat(outputOf("debug::debug_expr(. + 1) | .depends_on_input", NumericType.getInstance())).isSameAs(BooleanType.getInstance());
	}
}
