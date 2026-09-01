package net.thisptr.jackson.jq.v2.core;

import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * A compiled jq query, ready to be run against any number of inputs.
 * <p>
 * A query may produce zero, one, or many output values for a single input; each is handed to
 * {@code output} in the order jq itself would print them, before {@code apply} returns.
 *
 * @param <JsonNode> the JSON node type
 */
public interface JsonQuery<JsonNode> {

	/**
	 * Runs this query against {@code in}, passing every output value to {@code output}.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param bindings values for the variables and functions the {@link Environment} declared without one
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if {@code bindings} does not match what the query was compiled against
	 */
	void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, Consumer<? super JsonNode> output) throws JsonQueryException;

	/**
	 * Runs this query against {@code in} with no bindings.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if it references a variable or function that was declared without a value
	 */
	default void apply(JsonNode in, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply(in, JsonQueryBindings.empty(), output);
	}
}
