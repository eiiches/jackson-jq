package net.thisptr.jackson.jq.v2.core;

import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

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
	 * @param options settings for this invocation, including the limits it runs under
	 * @param bindings values for the variables and functions the {@link Environment} declared without one
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if {@code bindings} does not match what the query was compiled against
	 * @throws RuntimeLimitExceededException if the query exceeds a limit set by {@code options}
	 */
	void apply(JsonNode in, RuntimeOptions options, JsonQueryBindings<JsonNode> bindings, Consumer<? super JsonNode> output) throws JsonQueryException;

	/**
	 * Runs this query against {@code in} with no bindings.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param options settings for this invocation, including the limits it runs under
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if it references a variable or function that was declared without a value
	 * @throws RuntimeLimitExceededException if the query exceeds a limit set by {@code options}
	 */
	default void apply(JsonNode in, RuntimeOptions options, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply(in, options, JsonQueryBindings.empty(), output);
	}

	/**
	 * Runs this query against {@code in} with default options.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param bindings values for the variables and functions the {@link Environment} declared without one
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if {@code bindings} does not match what the query was compiled against
	 */
	default void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply(in, RuntimeOptions.DEFAULT, bindings, output);
	}

	/**
	 * Runs this query against {@code in} with default options and no bindings.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if it references a variable or function that was declared without a value
	 */
	default void apply(JsonNode in, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply(in, RuntimeOptions.DEFAULT, JsonQueryBindings.empty(), output);
	}
}
