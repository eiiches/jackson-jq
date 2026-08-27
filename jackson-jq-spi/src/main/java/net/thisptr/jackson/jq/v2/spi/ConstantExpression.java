package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

/**
 * An expression whose complete, input-independent result has been computed ahead of time.
 * <p>
 * Consumers may use this interface during {@link Function#bindArguments} to specialize costly
 * operations. The returned list is ordered and must not be modified. Its JSON nodes must also be
 * treated as read-only.
 *
 * @param <Context> the evaluation context type
 * @param <JsonNode> the JSON node type
 */
public interface ConstantExpression<Context, JsonNode> extends Expression<Context, JsonNode> {
	/**
	 * Returns all values emitted by this expression, in emission order.
	 *
	 * @return the emitted values, in emission order
	 */
	List<JsonNode> getConstantResults();

	@Override
	default Cardinality getCardinality() {
		int size = getConstantResults().size();
		if (size == 0)
			return Cardinality.ZERO;
		if (size == 1)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	@Override
	default boolean dependsOnInput() {
		return false;
	}

	@Override
	default boolean dependsOnExternalState() {
		return false;
	}

}
