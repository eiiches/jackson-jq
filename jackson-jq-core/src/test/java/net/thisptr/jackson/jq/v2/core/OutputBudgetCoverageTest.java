package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)} has no blind spots, one construct at
 * a time.
 * <p>
 * The budget is enforced by whichever sink receives an expression's values, so every construct that consumes
 * a value stream has to charge what it consumes: the node's own lambda for everything the engine consumes
 * itself, {@code MeteredOutputExpression} for an argument handed to a function, {@code RootExpression} for the
 * query's final output. A construct that forgets is a silent hole -- a query that runs forever under a budget
 * the caller believed in -- rather than a failing assertion anywhere else. Hence this test: one runaway per
 * construct, each routed so that the stream can only reach the budget through the construct under test.
 * <p>
 * Add a case here whenever a node starts consuming a stream of its own.
 * <p>
 * Every runaway is written as {@code range(0; $n)} rather than {@code range(0; 1000)} so that no case is a
 * constant expression. A constant one would be folded at compile time and never evaluated, so it would not
 * charge this budget and the case would stop testing anything -- see {@code RuntimeOptions}' class javadoc on
 * what the budgets meter. {@code $n} is declared, not defined, which makes it opaque to the folder.
 */
public class OutputBudgetCoverageTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Environment<JsonNode> ENV = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
			.declareVariable("n")
			.build();
	private static final long BUDGET = 10;
	private static final int STREAM_LENGTH = 1000;

	static Stream<Arguments> constructs() {
		return Stream.of(
				Arguments.of("pipe", "null", "range(0; $n) | empty"),
				Arguments.of("comma", "null", "(range(0; $n), 1) | empty"),
				Arguments.of("variable binding", "null", "range(0; $n) as $x | empty"),
				Arguments.of("conditional condition", "null", "if (range(0; $n) > 0) then 1 else 2 end | empty"),
				Arguments.of("reduce source", "null", "reduce range(0; $n) as $x (0; .)"),
				Arguments.of("reduce init", "null", "reduce empty as $x (range(0; $n); .) | empty"),
				Arguments.of("reduce update", "null", "reduce (1, 2) as $x (0; range(0; $n))"),
				Arguments.of("foreach source", "null", "foreach range(0; $n) as $x (0; .) | empty"),
				Arguments.of("foreach update", "null", "foreach (1, 2) as $x (0; range(0; $n)) | empty"),
				Arguments.of("array construction", "null", "[range(0; $n)] | length"),
				Arguments.of("object construction value", "null", "{a: range(0; $n)} | empty"),
				Arguments.of("object construction key", "null", "{(range(0; $n) | tostring): 1} | empty"),
				Arguments.of("string interpolation", "null", "\"\\(range(0; $n))\" | empty"),
				Arguments.of("arithmetic operator", "null", "(range(0; $n) + 0) | empty"),
				Arguments.of("boolean operator", "null", "(true and (range(0; $n) > 0)) | empty"),
				Arguments.of("alternative operator", "null", "(range(0; $n) // 1) | empty"),
				Arguments.of("assignment", "{\"a\":1}", "(.a = range(0; $n)) | empty"),
				Arguments.of("update assignment", "{\"a\":1}", "(.a |= (range(0; $n) | first(.))) | empty"),
				Arguments.of("field access target", "null", "(range(0; $n) | {a: 1}).a | empty"),
				Arguments.of("string field access key", "{\"a\":1}", ".[(range(0; $n) | tostring)] | empty"),
				Arguments.of("bracket field access index", "[1,2,3]", ".[range(0; $n) % 3] | empty"),
				Arguments.of("bracket field access range", "[1,2,3]", ".[range(0; $n) % 3 : 3] | empty"),
				Arguments.of("object destructuring key", "{\"a\":1}", ". as {(range(0; $n) | tostring): $v} | empty"),
				Arguments.of("root output", "null", "range(0; $n)"),
				Arguments.of("argument to a Java builtin", "[1,2,3]", "sort_by(range(0; $n)) | empty"),
				Arguments.of("argument to a jq-library builtin", "[1,2,3]", "map(range(0; $n)) | empty"),
				// The odd one out: nothing downstream ever sees a value, so only charging the caller's own
				// re-evaluated arguments catches it. See RuntimeOptionsTest#aLoopThatEmitsNothingIsStillBounded.
				Arguments.of("a loop that emits nothing", "null", "until(false; .)"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("constructs")
	public void everyStreamConsumingConstructChargesWhatItConsumes(String construct, String input, String query) throws Exception {
		RuntimeOptions options = RuntimeOptions.newBuilder().setMaxOutputsPerExpression(BUDGET).build();
		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setVariable("n", Jackson2JsonProvider.getInstance().createNumber(STREAM_LENGTH))
				.build();
		JsonNode in = MAPPER.readTree(input);
		assertThatThrownBy(() -> {
			List<JsonNode> out = new ArrayList<>();
			ENV.compile(query).withRuntimeOptions(options).withRuntimeBindings(bindings).apply(in, out::add);
		})
				.describedAs("%s does not charge the stream it consumes", construct)
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of " + BUDGET + " outputs per expression");
	}
}
