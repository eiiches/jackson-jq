package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;

import static org.assertj.core.api.Assertions.assertThat;

// end-to-end test of custom function using Environment
public class CustomFunctionTest {

	@Test
	public void testCustomFunction() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		Version version = Versions.JQ_1_6;

		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), version);

		env.addFunctionFactory(FunctionNameAndArity.of("times100", 1), new FunctionFactory() {
			@Override
			public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version ver) {
				return (scope, in, path, output) -> {
					args.get(0).apply(scope, in, (numberNode) -> {
						int n = jsonProvider.asInt(numberNode);
						output.emit(jsonProvider.createNumber(n * 100), null);
					});
				};
			}
		});

		String input = "{ \"a\": 5 }";

		JsonQuery<JsonNode> query = env.compile("{ \"a\": times100(.a) }");

		List<JsonNode> out = new ArrayList<>();
		query.apply(mapper.readTree(input), (outNode, path) -> out.add(outNode));
		assertThat(out).hasSize(1);
		assertThat(out.get(0)).isInstanceOf(ObjectNode.class);
		assertThat(out.get(0).toString()).isEqualTo("{\"a\":500}");
	}
}
