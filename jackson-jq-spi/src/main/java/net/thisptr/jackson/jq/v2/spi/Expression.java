package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * An executable jq expression.
 * <p>
 * Instances (however produced, e.g. via {@link Function#bindArguments}) must be safe for
 * concurrent, repeated calls to {@link #apply} for the lifetime of the compiled query -- see
 * {@link Function#bindArguments} for the corresponding guarantee from the producer side.
 *
 * @param <JsonNode> the JSON node type
 * @param <Context> an opaque object representing execution state that has to be passed on when evaluating function arguments
 */
public interface Expression<Context, JsonNode> {
	/**
	 * Returns the number of values this expression is known to emit for one input on normal
	 * completion.
	 * <p>
	 * The answer must be conservative -- return {@link Cardinality#UNKNOWN} rather than guess when
	 * unsure -- and stable for the lifetime of this instance, since compilers may cache it.
	 *
	 * @return the known output cardinality, or {@link Cardinality#UNKNOWN} when it cannot be proven
	 */
	default Cardinality getCardinality() {
		return Cardinality.UNKNOWN;
	}

	/**
	 * Whether this expression's result can vary depending on the {@code .}/path it's applied to.
	 * <p>
	 * The answer must be conservative -- {@code true} is always safe, so return it rather than
	 * guess when unsure -- and stable for the lifetime of this instance, since compilers may cache
	 * it.
	 *
	 * @return {@code true} if this expression's result can vary with the input
	 */
	default boolean dependsOnInput() {
		return true;
	}

	/**
	 * Whether this expression's result can vary due to external, non-deterministic state -- the
	 * JVM's RNG state, wall-clock time, the filesystem, the network, etc., rather than purely from its
	 * own {@code .}/path and bound arguments. References to jq variables such as {@code $var} are not
	 * considered external state.
	 * <p>
	 * The answer must be conservative -- {@code true} is always safe, so return it rather than
	 * guess when unsure -- and stable for the lifetime of this instance, since compilers may cache
	 * it.
	 *
	 * @return {@code true} if this expression's result can vary due to external state
	 */
	default boolean dependsOnExternalState() {
		return true;
	}

	/**
	 * Evaluates this expression against the given input.
	 *
	 * @param context an opaque object that has to be passed on when evaluating Function arguments
	 * @param in the input JSON node (the {@code .} context)
	 * @param ipath the path of the input JSON node, or {@link UntrackedPath#getInstance()} if untracked
	 * @param output the consumer to receive output JSON nodes
	 * @throws JsonQueryException if an error occurs during evaluation
	 */
	void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException;
}
