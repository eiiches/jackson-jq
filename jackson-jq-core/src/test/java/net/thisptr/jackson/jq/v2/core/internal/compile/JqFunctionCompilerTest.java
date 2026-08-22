package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class JqFunctionCompilerTest {
	@Test
	public void definitionKeyIsStructural() {
		JqFunction first = new JqFunction("identity", Collections.singletonList("f"), "f", null);
		JqFunction second = new JqFunction("identity", Collections.singletonList("f"), "f", null);

		JqFunctionCompiler.DefinitionKey firstKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, FunctionSignature.of("identity", 1), first, JqFunctionCompiler.Origin.LOADER);
		JqFunctionCompiler.DefinitionKey secondKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, FunctionSignature.of("identity", 1), second, JqFunctionCompiler.Origin.LOADER);

		assertThat(firstKey).isEqualTo(secondKey).hasSameHashCodeAs(secondKey);
	}

	@Test
	public void definitionKeyIncludesOrigin() {
		JqFunction definition = new JqFunction("identity", Collections.singletonList("f"), "f", null);
		FunctionSignature signature = FunctionSignature.of("identity", 1);

		JqFunctionCompiler.DefinitionKey environmentKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, signature, definition, JqFunctionCompiler.Origin.ENVIRONMENT);
		JqFunctionCompiler.DefinitionKey loaderKey = new JqFunctionCompiler.DefinitionKey(Versions.JQ_1_6, signature, definition, JqFunctionCompiler.Origin.LOADER);

		assertThat(environmentKey).isNotEqualTo(loaderKey);
	}
}
