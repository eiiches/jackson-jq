package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class JqFunctionCompilerTest {
	private static final Jackson2JsonProvider JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	@Test
	public void definitionKeyIsStructural() {
		JqFunction first = JqFunction.of("identity", Collections.singletonList(FunctionParameter.ofFilter("f")), "f");
		JqFunction second = JqFunction.of("identity", Collections.singletonList(FunctionParameter.ofFilter("f")), "f");

		JqFunctionCompiler.DefinitionKey firstKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, FunctionSignature.of("identity", 1), first, JqFunctionCompiler.Origin.LOADER);
		JqFunctionCompiler.DefinitionKey secondKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, FunctionSignature.of("identity", 1), second, JqFunctionCompiler.Origin.LOADER);

		assertThat(firstKey).isEqualTo(secondKey).hasSameHashCodeAs(secondKey);
	}

	@Test
	public void definitionKeyIncludesOrigin() {
		JqFunction definition = JqFunction.of("identity", Collections.singletonList(FunctionParameter.ofFilter("f")), "f");
		FunctionSignature signature = FunctionSignature.of("identity", 1);

		JqFunctionCompiler.DefinitionKey environmentKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, signature, definition, JqFunctionCompiler.Origin.ENVIRONMENT);
		JqFunctionCompiler.DefinitionKey loaderKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, signature, definition, JqFunctionCompiler.Origin.LOADER);

		assertThat(environmentKey).isNotEqualTo(loaderKey);
	}

	@Test
	public void valueArgumentsContributeToCardinality() {
		Environment<JsonNode> env = environment(FunctionParameter.ofValue("x"));

		assertThat(env.compile("f(empty)").getProperties().cardinality()).isEqualTo(Cardinality.ZERO);
		assertThat(env.compile("f(1)").getProperties().cardinality()).isEqualTo(Cardinality.ONE);
		assertThat(env.compile("f(1, 2)").getProperties().cardinality()).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void unusedValueArgumentsContributeToDependencies() {
		Environment<JsonNode> env = environment(FunctionParameter.ofValue("x"));

		assertThat(env.compile("f(.)").getProperties()).isEqualTo(new ExpressionProperties(Cardinality.ONE, true, false));
		assertThat(env.compile("f(now)").getProperties()).isEqualTo(new ExpressionProperties(Cardinality.ONE, false, true));
	}

	@Test
	public void unusedFilterArgumentsDoNotContributeToProperties() {
		Environment<JsonNode> env = environment(FunctionParameter.ofFilter("x"));

		assertThat(env.compile("f(1, 2)").getProperties()).isEqualTo(new ExpressionProperties(Cardinality.ONE, false, false));
		assertThat(env.compile("f(.)").getProperties()).isEqualTo(new ExpressionProperties(Cardinality.ONE, false, false));
		assertThat(env.compile("f(now)").getProperties()).isEqualTo(new ExpressionProperties(Cardinality.ONE, false, false));
	}

	private static Environment<JsonNode> environment(FunctionParameter parameter) {
		return EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2)
				.defineJqFunction(JqFunction.of("f", List.of(parameter), "7"))
				.build();
	}
}
