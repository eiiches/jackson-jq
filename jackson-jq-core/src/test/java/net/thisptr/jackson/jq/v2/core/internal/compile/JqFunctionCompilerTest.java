package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class JqFunctionCompilerTest {
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
}
