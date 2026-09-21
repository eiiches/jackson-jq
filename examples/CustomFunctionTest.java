package examples;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomFunctionTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void doubleExample() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("double", 0), new Function() {
					@Override
					public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
						return new ExpressionProperties(Cardinality.ONE, true, false);
					}

					@Override
					public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> arguments) {
						JsonProvider<JsonNode> provider = bindCtx.getJsonProvider();
						return new Expression<>() {


							@Override
							public void apply(Context context, JsonNode input, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
								int number = Objects.requireNonNull(provider.getNumberAsIntExact(input));
								output.emit(provider.createNumber(number * 2), UntrackedPath.getInstance());
							}
						};
					}
				})
				.build();

		JsonQuery<JsonNode> query = environment.compile("double");
		assertThat(query.apply(IntNode.valueOf(5))).containsExactly(IntNode.valueOf(10));
	}

	@Test
	public void assertExample() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("assert", 1), new Function() {
					@Override
					public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
						ExpressionProperties condition = arguments.get(0);
						return new ExpressionProperties(condition.cardinality(), true, condition.dependsOnExternalState());
					}

					@Override
					public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> arguments) {
						JsonProvider<JsonNode> provider = bindCtx.getJsonProvider();
						Expression<Context, JsonNode> condition = arguments.get(0);
						return new Expression<>() {


							@Override
							public void apply(Context context, JsonNode input, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
								condition.apply(context, input, path, (result, resultPath) -> {
									if (provider.isNull(result) || (provider.isBoolean(result) && !provider.getBoolean(result)))
										throw new JsonQueryException("assertion failed");
									output.emit(input, path);
								});
							}
						};
					}
				})
				.build();

		JsonQuery<JsonNode> query = environment.compile("assert(.value > 0)");

		JsonNode input = MAPPER.readTree("{\"value\":5}");
		assertThat(query.apply(input)).containsExactly(input);

		assertThatThrownBy(() -> query.apply(MAPPER.readTree("{\"value\":-1}")))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("assertion failed");
	}

	@Test
	public void heapUsageExample() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("heap_usage", 0), new Function() {
					@Override
					public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
						return new ExpressionProperties(Cardinality.ONE, false, true);
					}

					@Override
					public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> arguments) {
						JsonProvider<JsonNode> provider = bindCtx.getJsonProvider();
						return new Expression<>() {


							@Override
							public void apply(Context context, JsonNode input, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
								Runtime runtime = Runtime.getRuntime();
								long committed = runtime.totalMemory();
								Map<String, JsonNode> result = new LinkedHashMap<>();
								result.put("used", provider.createNumber(committed - runtime.freeMemory()));
								result.put("committed", provider.createNumber(committed));
								result.put("max", provider.createNumber(runtime.maxMemory()));
								output.emit(provider.createObject(result), UntrackedPath.getInstance());
							}
						};
					}
				})
				.build();

		JsonQuery<JsonNode> query = environment.compile("heap_usage | .used >= 0 and .used <= .committed and .committed <= .max");
		assertThat(query.apply(MAPPER.nullNode())).containsExactly(BooleanNode.TRUE);
	}
}
