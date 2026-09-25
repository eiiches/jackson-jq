package net.thisptr.jackson.jq.v2.core.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionPropertiesUtilsTest {
	@Test
	public void forwardAllCombinesCardinalityAndDependencies() {
		ExpressionProperties pureOne = new ExpressionProperties(Cardinality.ONE, false, false);
		ExpressionProperties pureZero = new ExpressionProperties(Cardinality.ZERO, false, false);
		ExpressionProperties pureUnknown = new ExpressionProperties(Cardinality.UNKNOWN, false, false);
		ExpressionProperties inputDependent = new ExpressionProperties(Cardinality.ONE, true, false);
		ExpressionProperties external = new ExpressionProperties(Cardinality.ONE, false, true);

		// Cardinality multiplication
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(pureOne, pureOne)).cardinality())
				.isEqualTo(Cardinality.ONE);
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(pureOne, pureZero)).cardinality())
				.isEqualTo(Cardinality.ZERO);
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(pureOne, pureUnknown)).cardinality())
				.isEqualTo(Cardinality.UNKNOWN);
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.UNKNOWN, false, false, List.of(pureZero)).cardinality())
				.isEqualTo(Cardinality.ZERO);
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.UNKNOWN, false, false, List.of(pureOne)).cardinality())
				.isEqualTo(Cardinality.UNKNOWN);
		assertThat(ExpressionPropertiesUtils.forwardAll(Cardinality.ZERO, false, false, List.of(pureOne)).cardinality())
				.isEqualTo(Cardinality.ZERO);

		// Dependency propagation
		ExpressionProperties propInput = ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(inputDependent));
		assertThat(propInput.dependsOnInput()).isTrue();
		assertThat(propInput.dependsOnExternalState()).isFalse();

		ExpressionProperties propExternal = ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(external));
		assertThat(propExternal.dependsOnInput()).isFalse();
		assertThat(propExternal.dependsOnExternalState()).isTrue();

		ExpressionProperties propBoth = ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, List.of(inputDependent, external));
		assertThat(propBoth.dependsOnInput()).isTrue();
		assertThat(propBoth.dependsOnExternalState()).isTrue();
	}

	@Test
	public void evaluateOnFixedInputSuppressesInputDependencyAndCombinesCardinality() {
		ExpressionProperties inputDependent = new ExpressionProperties(Cardinality.ONE, true, false);
		ExpressionProperties external = new ExpressionProperties(Cardinality.ONE, false, true);
		ExpressionProperties zero = new ExpressionProperties(Cardinality.ZERO, false, false);

		ExpressionProperties fixed = ExpressionPropertiesUtils.evaluateOnFixedInput(Cardinality.ONE, false, false,
				List.of(inputDependent, external));
		assertThat(fixed.cardinality()).isEqualTo(Cardinality.ONE);
		assertThat(fixed.dependsOnInput()).isFalse();
		assertThat(fixed.dependsOnExternalState()).isTrue();

		ExpressionProperties fixedZero = ExpressionPropertiesUtils.evaluateOnFixedInput(Cardinality.ONE, false, false,
				List.of(inputDependent, zero));
		assertThat(fixedZero.cardinality()).isEqualTo(Cardinality.ZERO);
	}

	@Test
	public void forwardDependenciesRetainsBaseCardinality() {
		ExpressionProperties zero = new ExpressionProperties(Cardinality.ZERO, true, false);
		ExpressionProperties external = new ExpressionProperties(Cardinality.UNKNOWN, false, true);

		ExpressionProperties result = ExpressionPropertiesUtils.forwardDependencies(Cardinality.ONE, false, false,
				List.of(zero, external));
		assertThat(result.cardinality()).isEqualTo(Cardinality.ONE);
		assertThat(result.dependsOnInput()).isTrue();
		assertThat(result.dependsOnExternalState()).isTrue();
	}
}
