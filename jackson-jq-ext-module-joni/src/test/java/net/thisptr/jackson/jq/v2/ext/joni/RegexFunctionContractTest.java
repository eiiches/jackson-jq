package net.thisptr.jackson.jq.v2.ext.joni;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegexFunctionContractTest {
	private static final Jackson2JsonProvider JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final ExpressionProperties PURE_ARGUMENT = new ExpressionProperties(Cardinality.ONE, false, false);
	private static final String IMPORT = "import \"jackson-jq/joni\" as joni; ";

	@Test
	public void testMatchImplFunctionContract() {
		MatchImplFunction fn = new MatchImplFunction();
		ExpressionProperties properties = fn.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT, PURE_ARGUMENT, PURE_ARGUMENT));
		assertThat(properties.dependsOnInput()).isTrue();
		assertThat(properties.dependsOnExternalState()).isFalse();
	}

	@Test
	public void testSubImplFunctionContract() {
		SubImplFunction fn = new SubImplFunction();
		ExpressionProperties properties = fn.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT, PURE_ARGUMENT, PURE_ARGUMENT));
		assertThat(properties.dependsOnInput()).isTrue();
		assertThat(properties.dependsOnExternalState()).isFalse();
	}

	@Test
	public void valueParameterSpecializationPreservesMultipleArgumentOutputs() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_7).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + "joni::splits((\"a\", \"b\"))");
		List<JsonNode> output = new ArrayList<>();
		query.apply(JSON_PROVIDER.createString("aba"), output::add);
		assertThat(output).extracting(JSON_PROVIDER::getString).containsExactly("", "b", "", "a", "a");
	}

	@Test
	public void invalidConstantRegexFallsBackToRuntimeEvaluation() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_7).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + "joni::test(\"[\"; \"\")");
		assertThatThrownBy(() -> query.apply(JSON_PROVIDER.createString("input"), ignored -> {
		}))
				.isInstanceOf(JsonQueryException.class);
	}
}
