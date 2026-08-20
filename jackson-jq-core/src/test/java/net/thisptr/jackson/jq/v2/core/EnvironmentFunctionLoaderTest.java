package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;

import static org.assertj.core.api.Assertions.assertThat;

// proves setFunctionLoader() actually takes effect on the built Environment.
public class EnvironmentFunctionLoaderTest {

	private static Function constantFunction(String text) {
		return new Function() {
			@Override
			public <N> Expression<N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version version) {
				return (frame, in, path, output, ignoredRequirePath) -> output.emit(jsonProvider.createString(text), null);
			}
		};
	}

	private static FunctionLoader constantLoader(FunctionSignature key, String text) {
		return version -> Collections.singletonMap(key, constantFunction(text));
	}

	@Test
	public void setFunctionLoaderAfterConstructionTakesEffect() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(FunctionSignature.of("greet", 0), "hello"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> out.add(val));

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("hello");
	}

	@Test
	public void setFunctionLoaderOverridesBuiltin() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(FunctionSignature.of("not", 0), "overridden"))
				.build();

		JsonQuery<JsonNode> query = env.compile("true | not");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> out.add(val));

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("overridden");
	}

	@Test
	public void explicitAddFunctionWinsOverFunctionLoader() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(key, "from-loader"))
				.addFunction(key, constantFunction("from-explicit"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> out.add(val));

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("from-explicit");
	}
}
