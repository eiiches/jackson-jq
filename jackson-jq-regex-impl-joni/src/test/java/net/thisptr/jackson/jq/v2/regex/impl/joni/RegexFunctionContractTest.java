package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.path.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegexFunctionContractTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	private static <T, Context> Expression<Context, T> pureExpression() {
		return new Expression<Context, T>() {
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
		Expression<Object, JsonNode> expr = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), Arrays.asList(pureExpression(), pureExpression(), pureExpression()), Versions.JQ_1_6);
		assertThat(expr.dependsOnInput()).isTrue();
		assertThat(expr.dependsOnExternalState()).isFalse();
	}

	@Test
	public void testSubImplFunctionContract() {
		_SubImplFunction fn = new _SubImplFunction();
		Expression<Object, JsonNode> expr = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), Arrays.asList(pureExpression(), pureExpression(), pureExpression()), Versions.JQ_1_6);
		assertThat(expr.dependsOnInput()).isTrue();
		assertThat(expr.dependsOnExternalState()).isFalse();
	}

	@Test
	public void valueParameterSpecializationPreservesMultipleArgumentOutputs() {
		Environment<JsonNode> environment = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7).build();
		JsonQuery<JsonNode> query = environment.compile("splits((\"a\", \"b\"))");
		List<JsonNode> output = new ArrayList<>();
		query.apply(JSON_PROVIDER.createString("aba"), output::add);
		assertThat(output).extracting(JSON_PROVIDER::asString).containsExactly("", "b", "", "a", "a");
	}

	@Test
	public void invalidConstantRegexFailsAtCompileTime() {
		Environment<JsonNode> environment = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7).build();
		assertThatThrownBy(() -> environment.compile("test(\"[\"; \"\")"))
				.isInstanceOf(RuntimeException.class);
	}
}
