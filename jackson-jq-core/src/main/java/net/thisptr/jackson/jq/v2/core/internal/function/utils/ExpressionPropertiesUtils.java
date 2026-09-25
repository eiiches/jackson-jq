package net.thisptr.jackson.jq.v2.core.internal.function.utils;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;

public final class ExpressionPropertiesUtils {
	private ExpressionPropertiesUtils() {
	}

	/**
	 * Propagates cardinality and both dependencies from every argument because each receives the call input.
	 */
	public static ExpressionProperties forwardAll(Cardinality cardinality, boolean readsInput,
												  boolean readsExternalState, List<ExpressionProperties> arguments) {
		@Var Cardinality combinedCardinality = cardinality;
		@Var boolean dependsOnInput = readsInput;
		@Var boolean dependsOnExternalState = readsExternalState;
		for (ExpressionProperties argument : arguments) {
			combinedCardinality = CardinalityUtils.multiply(combinedCardinality, argument.cardinality());
			dependsOnInput |= argument.dependsOnInput();
			dependsOnExternalState |= argument.dependsOnExternalState();
		}
		return new ExpressionProperties(combinedCardinality, dependsOnInput, dependsOnExternalState);
	}

	/**
	 * Propagates external state and cardinality from evaluated arguments, but not input dependency.
	 */
	public static ExpressionProperties evaluateOnFixedInput(Cardinality cardinality, boolean readsInput,
															boolean readsExternalState, List<ExpressionProperties> arguments) {
		@Var Cardinality combinedCardinality = cardinality;
		@Var boolean dependsOnExternalState = readsExternalState;
		for (ExpressionProperties argument : arguments) {
			combinedCardinality = CardinalityUtils.multiply(combinedCardinality, argument.cardinality());
			dependsOnExternalState |= argument.dependsOnExternalState();
		}
		return new ExpressionProperties(combinedCardinality, readsInput, dependsOnExternalState);
	}

	/**
	 * Propagates both dependencies from every argument receiving the call input, but retains the base
	 * cardinality without multiplying by argument cardinalities. Useful for higher-order aggregation
	 * functions (e.g. {@code sort_by}, {@code group_by}) whose result cardinality is independent of their
	 * filter parameter's cardinality.
	 */
	public static ExpressionProperties forwardDependencies(Cardinality cardinality, boolean readsInput,
														   boolean readsExternalState, List<ExpressionProperties> arguments) {
		@Var boolean dependsOnInput = readsInput;
		@Var boolean dependsOnExternalState = readsExternalState;
		for (ExpressionProperties argument : arguments) {
			dependsOnInput |= argument.dependsOnInput();
			dependsOnExternalState |= argument.dependsOnExternalState();
		}
		return new ExpressionProperties(cardinality, dependsOnInput, dependsOnExternalState);
	}
}
