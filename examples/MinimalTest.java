package examples;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class MinimalTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void evaluatesQuery() {
		Environment<JsonNode> environment = new EnvironmentBuilder<>(Jackson3JsonProviderImpl.getInstance(), Versions.JQ_1_8_2).build();

		JsonQuery<JsonNode> query = environment.compile(".name");

		JsonNode input = MAPPER.readTree("{\"name\":\"foo\"}");
		List<JsonNode> output = new ArrayList<>();
		query.apply(input, output::add);
		assertThat(output).containsExactly(StringNode.valueOf("foo"));
	}
}
