package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;

/**
 * Conservative analysis of an {@link Expression}. Each component is a guarantee: an implementation
 * may report less precision than it can prove, but must not omit a possible dependency or understate the
 * number of results.
 *
 * @param cardinality the number of values emitted on normal completion
 * @param dependsOnInput whether the result can vary with the call input or its path
 * @param dependsOnExternalState whether the result can vary due to external state
 */
public record ExpressionProperties(Cardinality cardinality, boolean dependsOnInput, boolean dependsOnExternalState) {
	/**
	 * The conservative result for an expression that does not provide analysis.
	 */
	public static final ExpressionProperties UNKNOWN = new ExpressionProperties(Cardinality.UNKNOWN, true, true);

	public ExpressionProperties {
		Objects.requireNonNull(cardinality, "cardinality");
	}
}
