package net.thisptr.jackson.jq.v2.ext.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeModuleTest {
	private static final ExpressionProperties PURE_ARGUMENT = new ExpressionProperties(Cardinality.ONE, false, false);

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
		ExpressionProperties strftime1Properties = strftime1.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT));
		assertThat(strftime1Properties.dependsOnInput()).isTrue();
		assertThat(strftime1Properties.dependsOnExternalState()).isTrue();
		Function strftime2 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strftime", 2)));
		ExpressionProperties strftime2Properties = strftime2.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT, PURE_ARGUMENT));
		assertThat(strftime2Properties.dependsOnInput()).isTrue();
		assertThat(strftime2Properties.dependsOnExternalState()).isFalse();
		Function strptime1 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strptime", 1)));
		ExpressionProperties strptime1Properties = strptime1.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT));
		assertThat(strptime1Properties.dependsOnInput()).isTrue();
		assertThat(strptime1Properties.dependsOnExternalState()).isTrue();
		Function strptime2 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("strptime", 2)));
		ExpressionProperties strptime2Properties = strptime2.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT, PURE_ARGUMENT));
		assertThat(strptime2Properties.dependsOnInput()).isTrue();
		assertThat(strptime2Properties.dependsOnExternalState()).isFalse();
		Function timestamp = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("timestamp", 0)));
		ExpressionProperties timestampProperties = timestamp.analyze(Versions.JQ_1_6, List.of());
		assertThat(timestampProperties.dependsOnInput()).isFalse();
		assertThat(timestampProperties.dependsOnExternalState()).isTrue();
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
