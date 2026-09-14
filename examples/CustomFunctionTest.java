package examples;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomFunctionTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void evaluatesCustomFunction() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("times100", 1), new Function() {
					@Override
					public <Context extends RuntimeContext, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> arguments, Version jqVersion) {
						return (context, input, path, output) -> arguments.get(0).apply(context, input, UntrackedPath.getInstance(), (value, outputPath) -> {
							int number = Objects.requireNonNull(provider.getNumberAsIntExact(value));
							output.emit(provider.createNumber(number * 100), UntrackedPath.getInstance());
						});
					}
				})
				.build();

		JsonQuery<JsonNode> query = environment.compile("times100(.value)");

		JsonNode input = MAPPER.readTree("{\"value\":5}");
		List<JsonNode> output = query.apply(input);
		assertThat(output).containsExactly(IntNode.valueOf(500));
	}
}
