package net.thisptr.jackson.jq.v2.ext.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeModuleTest {
	private static final BindContext<JsonNode> BIND_CONTEXT = new BindContext<JsonNode>() {
		@Override
		public JsonProvider<JsonNode> getJsonProvider() {
			return Jackson2JsonProvider.getInstance();
		}

		@Override
		public Version getJqVersion() {
			return Versions.JQ_1_6;
		}
	};

	private static <T, Context extends RuntimeContext> Expression<Context, T> pureExpression() {
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
	public void formatsAndParsesTime() throws JsonQueryException {
		assertThat(run("1477162342372 | ext::strftime(\"yyyy-MM-dd HH:mm:ss.SSSXXX\"; \"UTC\")"))
				.extracting(JsonNode::textValue)
				.containsExactly("2016-10-22 18:52:22.372Z");
		assertThat(run("\"2016-10-22 18:52:22.372\" | ext::strptime(\"yyyy-MM-dd HH:mm:ss.SSS\"; \"UTC\")"))
				.extracting(JsonNode::longValue)
				.containsExactly(1477162342372L);
		assertThat(run("1477162342372 | ext::strftime(\"yyyy-MM-dd HH:mm:ss.SSS\") | ext::strptime(\"yyyy-MM-dd HH:mm:ss.SSS\")"))
				.extracting(JsonNode::longValue)
				.containsExactly(1477162342372L);
	}

	@Test
	public void rejectsNonFiniteEpoch() {
		assertThatThrownBy(() -> run("infinite | ext::strftime(\"yyyy\")")).isInstanceOf(JsonQueryException.class);
		assertThatThrownBy(() -> run("-infinite | ext::strftime(\"yyyy\")")).isInstanceOf(JsonQueryException.class);
		assertThatThrownBy(() -> run("nan | ext::strftime(\"yyyy\")")).isInstanceOf(JsonQueryException.class);
		assertThatThrownBy(() -> run("1e300 | ext::strftime(\"yyyy\")")).isInstanceOf(JsonQueryException.class);
	}

	@Test
	public void returnsTheCurrentTimestamp() throws JsonQueryException {
		long before = System.currentTimeMillis();
		List<JsonNode> result = run("ext::timestamp");
		long after = System.currentTimeMillis();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).longValue()).isBetween(before, after);
	}

	@Test
	public void exposesFunctions() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("strftime", 1),
				FunctionSignature.of("strftime", 2),
				FunctionSignature.of("strptime", 1),
				FunctionSignature.of("strptime", 2),
				FunctionSignature.of("timestamp", 0));
	}

	@Test
	public void functionContract() {
		ModuleImpl module = new ModuleImpl();
		Function strftime1 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strftime", 1)));
		Expression<RuntimeContext, JsonNode> strftime1Expr = strftime1.bind(BIND_CONTEXT, Collections.singletonList(pureExpression()));
		assertThat(strftime1Expr.dependsOnInput()).isTrue();
		assertThat(strftime1Expr.dependsOnExternalState()).isTrue();
		Function strftime2 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strftime", 2)));
		Expression<RuntimeContext, JsonNode> strftime2Expr = strftime2.bind(BIND_CONTEXT, List.of(pureExpression(), pureExpression()));
		assertThat(strftime2Expr.dependsOnInput()).isTrue();
		assertThat(strftime2Expr.dependsOnExternalState()).isFalse();
		Function strptime1 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strptime", 1)));
		Expression<RuntimeContext, JsonNode> strptime1Expr = strptime1.bind(BIND_CONTEXT, Collections.singletonList(pureExpression()));
		assertThat(strptime1Expr.dependsOnInput()).isTrue();
		assertThat(strptime1Expr.dependsOnExternalState()).isTrue();
		Function strptime2 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strptime", 2)));
		Expression<RuntimeContext, JsonNode> strptime2Expr = strptime2.bind(BIND_CONTEXT, List.of(pureExpression(), pureExpression()));
		assertThat(strptime2Expr.dependsOnInput()).isTrue();
		assertThat(strptime2Expr.dependsOnExternalState()).isFalse();
		Function timestamp = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("timestamp", 0)));
		Expression<RuntimeContext, JsonNode> timestampExpr = timestamp.bind(BIND_CONTEXT, Collections.emptyList());
		assertThat(timestampExpr.dependsOnInput()).isFalse();
		assertThat(timestampExpr.dependsOnExternalState()).isTrue();
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/time\" as ext; " + expression);
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), results::add);
		return results;
	}
}
