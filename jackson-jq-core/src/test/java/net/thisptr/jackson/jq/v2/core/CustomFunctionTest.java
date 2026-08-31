package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;

// end-to-end test of custom function using Environment
public class CustomFunctionTest {

	@Test
	public void testCustomFunction() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		Version version = Versions.JQ_1_6;

		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), version)
				.defineFunction(FunctionSignature.of("times100", 1), new Function() {
					@Override
					public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, Version ver) {
						return (frame, in, path, output) -> {
							args.get(0).apply(frame, in, UntrackedPath.getInstance(), (numberNode, opath) -> {
								int n = Objects.requireNonNull(jsonProvider.getNumberAsIntExact(numberNode));
								output.emit(jsonProvider.createNumber(n * 100), UntrackedPath.getInstance());
							});
						};
					}
				})
				.build();

		String input = "{ \"a\": 5 }";

		JsonQuery<JsonNode> query = env.compile("{ \"a\": times100(.a) }");

		List<JsonNode> out = new ArrayList<>();
		query.apply(mapper.readTree(input), out::add);
		assertThat(out).hasSize(1);
		assertThat(out.get(0)).isInstanceOf(ObjectNode.class);
		assertThat(out.get(0).toString()).isEqualTo("{\"a\":500}");
	}
}
