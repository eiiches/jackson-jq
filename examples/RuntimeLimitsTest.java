package examples;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuntimeLimitsTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void limitsArrayLength() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(". + [4]");

		RuntimeOptions options = RuntimeOptions.newBuilder()
				.setMaxArrayLength(3)
				.build();

		JsonNode input = MAPPER.readTree("[1, 2, 3]");
		assertThatThrownBy(() -> query.withRuntimeOptions(options).apply(input))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum array size of 3");
	}

	@Test
	public void limitsObjectMemberCount() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(". + {d: 4}");

		RuntimeOptions options = RuntimeOptions.newBuilder()
				.setMaxObjectMemberCount(3)
				.build();

		JsonNode input = MAPPER.readTree("{\"a\": 1, \"b\": 2, \"c\": 3}");
		assertThatThrownBy(() -> query.withRuntimeOptions(options).apply(input))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum object size of 3");
	}

	@Test
	public void limitsStringLength() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(". + \"d\"");

		RuntimeOptions options = RuntimeOptions.newBuilder()
				.setMaxStringLength(3)
				.build();

		JsonNode input = MAPPER.readTree("\"abc\"");
		assertThatThrownBy(() -> query.withRuntimeOptions(options).apply(input))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum string length of 3");
	}

	@Test
	public void limitsUserDefinedFunctionCalls() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		// Without a budget this recurses until the Java stack runs out.
		JsonQuery<JsonNode> query = environment.compile("def countdown: if . > 0 then . - 1 | countdown else . end; countdown");

		RuntimeOptions options = RuntimeOptions.newBuilder()
				.setMaxUserDefinedFunctionCalls(3)
				.build();

		JsonNode input = MAPPER.readTree("1000000");
		assertThatThrownBy(() -> query.withRuntimeOptions(options).apply(input))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 3 user-defined function calls");
	}

	@Test
	public void limitsOutputsPerExpression() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		// Without a budget this streams ten million values before producing its single answer.
		JsonQuery<JsonNode> query = environment.compile("reduce range(0; 10000000) as $x (0; . + 1)");

		RuntimeOptions options = RuntimeOptions.newBuilder()
				.setMaxOutputsPerExpression(100)
				.build();

		JsonNode input = MAPPER.readTree("null");
		assertThatThrownBy(() -> query.withRuntimeOptions(options).apply(input))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 100 outputs per expression");
	}
}
