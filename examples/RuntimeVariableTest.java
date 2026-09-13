package examples;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeVariableTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void reusesQueryWithDifferentBindings() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.declareVariable("multiplier")
				.build();

		JsonQuery<JsonNode> query = environment.compile(".value * $multiplier");
		JsonNode input = MAPPER.readTree("{\"value\":21}");

		RuntimeBindings<JsonNode> fixedBindings = RuntimeBindings.<JsonNode>newBuilder()
				.setVariable("multiplier", IntNode.valueOf(2))
				.build();
		List<JsonNode> fixedOutput = new ArrayList<>();
		query.withRuntimeBindings(fixedBindings).apply(input, fixedOutput::add);
		assertThat(fixedOutput).containsExactly(IntNode.valueOf(42));

		RuntimeBindings<JsonNode> suppliedBindings = RuntimeBindings.<JsonNode>newBuilder()
				.setVariable("multiplier", () -> MAPPER.valueToTree(3))
				.build();
		List<JsonNode> suppliedOutput = new ArrayList<>();
		query.withRuntimeBindings(suppliedBindings).apply(input, suppliedOutput::add);
		assertThat(suppliedOutput).containsExactly(IntNode.valueOf(63));
	}
}
