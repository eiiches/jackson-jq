package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;

/**
 * A call to a jq-source function that is calling itself: the body it will run is the one still being
 * compiled, so it is resolved at run time rather than held here.
 * <p>
 * This is what {@code flatten} and {@code combinations} compile to at their recursive call sites. Unlike a
 * {@link BoundJqFunctionCall} there is no body for an analysis to look at -- only the arguments.
 *
 * @param <JsonNode> the JSON node type
 */
public interface DeferredJqFunctionCall<JsonNode> extends AnalyzedExpression<JsonNode> {
	/**
	 * The argument expressions this call site passes.
	 */
	List<AnalyzedExpression<JsonNode>> arguments();

	@Override
	default boolean dependsOnExternalState() {
		return arguments().stream().anyMatch(AnalyzedExpression::dependsOnExternalState);
	}
}
