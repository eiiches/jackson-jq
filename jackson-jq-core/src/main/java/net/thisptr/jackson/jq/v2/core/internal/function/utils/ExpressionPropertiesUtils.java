package net.thisptr.jackson.jq.v2.core.internal.function.utils;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;

public final class ExpressionPropertiesUtils {
	private ExpressionPropertiesUtils() {
	}

	/**
	 * Propagates both dependencies from every argument because each receives the call input.
	 */
	public static ExpressionProperties forwardAll(Cardinality cardinality, boolean readsInput,
												  boolean readsExternalState, List<ExpressionProperties> arguments) {
		@Var boolean dependsOnInput = readsInput;
		@Var boolean dependsOnExternalState = readsExternalState;
		for (ExpressionProperties argument : arguments) {
			dependsOnInput |= argument.dependsOnInput();
			dependsOnExternalState |= argument.dependsOnExternalState();
		}
		return new ExpressionProperties(cardinality, dependsOnInput, dependsOnExternalState);
	}

	/**
	 * Propagates external state from evaluated arguments, but not input dependency.
	 */
	public static ExpressionProperties evaluateOnFixedInput(Cardinality cardinality, boolean readsInput,
															boolean readsExternalState, List<ExpressionProperties> arguments) {
		@Var boolean dependsOnExternalState = readsExternalState;
		for (ExpressionProperties argument : arguments)
			dependsOnExternalState |= argument.dependsOnExternalState();
		return new ExpressionProperties(cardinality, readsInput, dependsOnExternalState);
	}
}
