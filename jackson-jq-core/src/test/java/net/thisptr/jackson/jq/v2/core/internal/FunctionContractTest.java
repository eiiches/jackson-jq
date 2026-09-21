package net.thisptr.jackson.jq.v2.core.internal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionContractTest {
	@Test
	public void argumentDependencyTransferDistinguishesItsEvaluationInput() {
		ExpressionProperties inputDependent = new ExpressionProperties(Cardinality.ONE, true, false);
		ExpressionProperties external = new ExpressionProperties(Cardinality.ONE, false, true);

		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(inputDependent)).dependsOnInput()).isTrue();
		ExpressionProperties fixedInput = ExpressionPropertiesUtils.evaluateOnFixedInput(Cardinality.ONE, false, false,
				List.of(inputDependent, external));
		assertThat(fixedInput.dependsOnInput()).isFalse();
		assertThat(fixedInput.dependsOnExternalState()).isTrue();

		ExpressionProperties unused = new ExpressionProperties(Cardinality.ONE, false, false);
		assertThat(unused.dependsOnInput()).isFalse();
		assertThat(unused.dependsOnExternalState()).isFalse();
	}

	@Test
	public void allCoreFunctionsDeclareConservativeProperties() {
		for (Version version : Versions.versions()) {
			Map<FunctionSignature, Function> functions = ClassPathFunctionLoader.getInstance().getFunctions(version);
			assertThat(functions).isNotEmpty();
			for (Map.Entry<FunctionSignature, Function> entry : functions.entrySet()) {
				FunctionSignature signature = entry.getKey();
				Function function = entry.getValue();
				try {
					assertThat(function.getClass().getMethod("analyze", Version.class, List.class).getDeclaringClass())
							.as("%s in %s overrides analyze", signature, version)
							.isNotEqualTo(Function.class);
				} catch (NoSuchMethodException e) {
					throw new AssertionError(e);
				}
				int arity = signature.arity() != null ? signature.arity() : 0;
				List<ExpressionProperties> pureArguments = java.util.Collections.nCopies(arity,
						new ExpressionProperties(Cardinality.ONE, false, false));
				ExpressionProperties properties = function.analyze(version, pureArguments);
				assertThat(properties).as("%s/%d in %s", signature.name(), arity, version).isNotNull();
				assertThat(properties.cardinality()).isNotNull();

				if (arity > 0) {
					List<ExpressionProperties> externalArguments = java.util.Collections.nCopies(arity,
							new ExpressionProperties(Cardinality.ONE, false, true));
					assertThat(function.analyze(version, externalArguments).dependsOnExternalState())
							.as("%s/%d propagates evaluated external-state arguments in %s", signature.name(), arity, version)
							.isTrue();
				}
			}
		}
	}
}
