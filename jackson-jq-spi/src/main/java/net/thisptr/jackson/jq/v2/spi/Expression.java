package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * An executable jq expression.
 *
 * @param <JsonNode> the JSON node type
 * @param <Context> an opaque object representing execution state that has to be passed on when evaluating function arguments
 */
public interface Expression<Context, JsonNode> {
	/**
	 * Returns the number of values this expression is known to emit for one input on normal
	 * completion.
	 *
	 * @return the known output cardinality, or {@link Cardinality#UNKNOWN} when it cannot be proven
	 */
	default Cardinality getCardinality() {
		return Cardinality.UNKNOWN;
	}

	/** Whether this expression's result can vary depending on the {@code .}/path it's applied to. */
	default boolean dependsOnInput() {
		return true;
	}

	/**
	 * Whether this expression's result can vary due to external, non-deterministic state -- the
	 * JVM's RNG state, wall-clock time, the filesystem, the network, etc., rather than purely from its
	 * own {@code .}/path and bound arguments. References to jq variables such as {@code $var} are not
	 * considered external state.
	 */
	default boolean dependsOnExternalState() {
		return true;
	}

	/**
	 * Evaluates this expression against the given input.
	 *
	 * @param context an opaque object that has to be passed on when evaluating Function arguments
	 * @param in the input JSON node (the {@code .} context)
	 * @param ipath the path of the input JSON node, or {@code null} if untracked
	 * @param output the consumer to receive output JSON nodes
	 * @throws JsonQueryException if an error occurs during evaluation
	 */
	void apply(Context context, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException;
}
