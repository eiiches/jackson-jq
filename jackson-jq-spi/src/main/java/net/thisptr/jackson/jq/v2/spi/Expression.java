package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * An executable jq expression.
 * <p>
 * Instances (however produced, e.g. via {@link Function#bind}) must be safe for
 * concurrent, repeated calls to {@link #apply} for the lifetime of the compiled query -- see
 * {@link Function#bind} for the corresponding guarantee from the producer side.
 *
 * @param <JsonNode> the JSON node type
 * @param <Context> the execution state that has to be passed on when evaluating function arguments; opaque
 * apart from the {@link RuntimeLimits} it exposes
 */
@FunctionalInterface
public interface Expression<Context extends RuntimeContext, JsonNode> {
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
