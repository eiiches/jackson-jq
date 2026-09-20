package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegexFunctionContractTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final BindContext<JsonNode> BIND_CONTEXT = new BindContext<>() {
		@Override
		public JsonProvider<JsonNode> getJsonProvider() {
			return JSON_PROVIDER;
		}

		@Override
		public Version getJqVersion() {
			return Versions.JQ_1_6;
		}
	};

	private static <T, Context extends RuntimeContext> Expression<Context, T> pureExpression() {
		return new Expression<>() {
			@Override
			public boolean dependsOnInput() {
				return false;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, T in, Path<T> ipath, Output<T> output) {
			}
		};
	}

	@Test
	public void testMatchImplFunctionContract() {
		_MatchImplFunction fn = new _MatchImplFunction();
		Expression<RuntimeContext, JsonNode> expr = fn.bind(BIND_CONTEXT, List.of(pureExpression(), pureExpression(), pureExpression()));
		assertThat(expr.dependsOnInput()).isTrue();
		assertThat(expr.dependsOnExternalState()).isFalse();
	}

	@Test
	public void testSubImplFunctionContract() {
		_SubImplFunction fn = new _SubImplFunction();
		Expression<RuntimeContext, JsonNode> expr = fn.bind(BIND_CONTEXT, List.of(pureExpression(), pureExpression(), pureExpression()));
		assertThat(expr.dependsOnInput()).isTrue();
		assertThat(expr.dependsOnExternalState()).isFalse();
	}

	@Test
	public void valueParameterSpecializationPreservesMultipleArgumentOutputs() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_7).build();
		JsonQuery<JsonNode> query = environment.compile("splits((\"a\", \"b\"))");
		List<JsonNode> output = new ArrayList<>();
		query.apply(JSON_PROVIDER.createString("aba"), output::add);
		assertThat(output).extracting(JSON_PROVIDER::getString).containsExactly("", "b", "", "a", "a");
	}

	@Test
	public void invalidConstantRegexFailsAtCompileTime() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_7).build();
		assertThatThrownBy(() -> environment.compile("test(\"[\"; \"\")"))
				.isInstanceOf(RuntimeException.class);
	}
}
