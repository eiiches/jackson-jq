package net.thisptr.jackson.jq.v2.fuzz;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonJqRunnerTest {
	private final JacksonJqRunner<JsonNode> runner = new JacksonJqRunner<>(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2);

	@Test
	void validatesAnActuallyCompiledQuery() throws Throwable {
		JacksonJqRunner.CheckedResult result = runner.evaluateChecked(". + 1", IntNode.valueOf(1), Duration.ofSeconds(5));
		assertThat(result.result().error()).isNull();
		assertThat(result.result().values()).containsExactly(IntNode.valueOf(2));
		assertThat(result.violations()).isEmpty();
	}

	@Test
	void checksEmittedValuesAgainstInferredType() {
		JacksonJqRunner.CheckedResult matching = run(StringType.getInstance(), Cardinality.ONE, List.of(TextNode.valueOf("ok")), false);
		assertThat(matching.violations()).isEmpty();

		JacksonJqRunner.CheckedResult mismatch = run(StringType.getInstance(), Cardinality.ONE, List.of(IntNode.valueOf(1)), false);
		assertThat(mismatch.violations()).singleElement().satisfies(violation -> {
			assertThat(violation.kind()).isEqualTo(JacksonJqRunner.InferenceKind.TYPE);
			assertThat(violation.message()).contains("index 0", "STRING");
		});
	}

	@Test
	void checksCardinalityOnNormalCompletion() {
		assertThat(run(AnyType.getInstance(), Cardinality.ZERO, List.of(), false).violations()).isEmpty();
		assertCardinalityMismatch(run(AnyType.getInstance(), Cardinality.ZERO, List.of(NullNode.getInstance()), false));
		assertCardinalityMismatch(run(AnyType.getInstance(), Cardinality.ONE, List.of(), false));
		assertThat(run(AnyType.getInstance(), Cardinality.ONE, List.of(NullNode.getInstance()), false).violations()).isEmpty();
		assertCardinalityMismatch(run(AnyType.getInstance(), Cardinality.ONE, List.of(NullNode.getInstance(), NullNode.getInstance()), false));
		assertThat(run(AnyType.getInstance(), Cardinality.UNKNOWN, List.of(NullNode.getInstance(), NullNode.getInstance()), false).violations()).isEmpty();
	}

	@Test
	void checksPartialOutputsBeforeRuntimeError() {
		assertThat(run(AnyType.getInstance(), Cardinality.ONE, List.of(), true).violations()).isEmpty();
		assertThat(run(AnyType.getInstance(), Cardinality.ONE, List.of(NullNode.getInstance()), true).violations()).isEmpty();
		assertCardinalityMismatch(run(AnyType.getInstance(), Cardinality.ONE, List.of(NullNode.getInstance(), NullNode.getInstance()), true));
		assertCardinalityMismatch(run(AnyType.getInstance(), Cardinality.ZERO, List.of(NullNode.getInstance()), true));
		assertThat(run(StringType.getInstance(), Cardinality.UNKNOWN, List.of(IntNode.valueOf(1)), true).violations())
				.singleElement().extracting(JacksonJqRunner.InferenceViolation::kind)
				.isEqualTo(JacksonJqRunner.InferenceKind.TYPE);
	}

	@Test
	void reportsTypeAndCardinalityViolationsTogether() {
		JacksonJqRunner.CheckedResult result = run(StringType.getInstance(), Cardinality.ZERO, List.of(IntNode.valueOf(1)), false);
		assertThat(result.violations()).extracting(JacksonJqRunner.InferenceViolation::kind)
				.containsExactly(JacksonJqRunner.InferenceKind.TYPE, JacksonJqRunner.InferenceKind.CARDINALITY);
	}

	private static void assertCardinalityMismatch(JacksonJqRunner.CheckedResult result) {
		assertThat(result.violations()).singleElement().extracting(JacksonJqRunner.InferenceViolation::kind)
				.isEqualTo(JacksonJqRunner.InferenceKind.CARDINALITY);
	}

	private JacksonJqRunner.CheckedResult run(Type outputType, Cardinality cardinality, List<JsonNode> outputs, boolean fail) {
		JsonQuery<JsonNode> query = new JsonQuery<>() {
			@Override
			public FilterType getType() {
				return FilterType.of(AnyType.getInstance(), outputType);
			}

			@Override
			public ExpressionProperties getProperties() {
				return new ExpressionProperties(cardinality, true, true);
			}

			@Override
			public JsonQuery<JsonNode> withRuntimeOptions(RuntimeOptions options) {
				return this;
			}

			@Override
			public JsonQuery<JsonNode> withRuntimeBindings(RuntimeBindings<JsonNode> bindings) {
				return this;
			}

			@Override
			public void apply(JsonNode in, Consumer<? super JsonNode> output) {
				outputs.forEach(output);
				if (fail)
					throw new JsonQueryException("runtime failure");
			}
		};
		return runner.doEvaluate(query, NullNode.getInstance());
	}
}
