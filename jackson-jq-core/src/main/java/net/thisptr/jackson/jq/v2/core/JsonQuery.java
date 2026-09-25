package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;

/**
 * A compiled jq query, ready to be run against any number of inputs.
 * <p>
 * A query may produce zero, one, or many output values for a single input; each is handed to
 * {@code output} in the order jq itself would print them, before {@code apply} returns.
 * <p>
 * Instances are immutable and safe to use from several threads at once.
 * {@link #withRuntimeOptions} and {@link #withRuntimeBindings} do not modify the query they are called
 * on: each returns a new query carrying that setting, sharing the compiled expression with the original.
 * There is no terminal build step -- every query along the way, configured or not, can be applied as it
 * is:
 *
 * <pre>{@code
 * JsonQuery<JsonNode> query = env.compile(".foo")
 *         .withRuntimeOptions(options)
 *         .withRuntimeBindings(bindings);
 * List<JsonNode> output = query.apply(in);
 * }</pre>
 * <p>
 * Pass a {@link Consumer} instead to receive each output value as it is produced, rather than holding the
 * whole result in memory:
 *
 * <pre>{@code
 * for (JsonNode in : inputs)
 *     query.apply(in, System.out::println);
 * }</pre>
 *
 * @param <JsonNode> the JSON node type
 */
public interface JsonQuery<JsonNode> {
	/**
	 * Returns the input/output type inferred when this query was compiled.
	 */
	FilterType getType();

	/**
	 * Returns the cardinality inferred when this query was compiled.
	 *
	 * @return the cardinality of this query
	 */
	Cardinality getCardinality();

	/**
	 * Returns a query that runs under {@code options}, replacing any previously set options.
	 *
	 * @param options settings for each invocation, including the limits it runs under
	 * @return a new query; this one is left unchanged
	 */
	JsonQuery<JsonNode> withRuntimeOptions(RuntimeOptions options);

	/**
	 * Returns a query that runs with {@code bindings}, replacing any previously set bindings.
	 * <p>
	 * The bindings are checked against the query as this is called, so a binding the query cannot accept
	 * is reported here rather than on the first input.
	 *
	 * @param bindings values for the variables and functions the {@link Environment} declared without one
	 * @return a new query; this one is left unchanged
	 * @throws JsonQueryException if {@code bindings} does not match what the query was compiled against
	 */
	JsonQuery<JsonNode> withRuntimeBindings(RuntimeBindings<JsonNode> bindings) throws JsonQueryException;

	/**
	 * Runs this query against {@code in}, passing every output value to {@code output}.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @param output receives each output value
	 * @throws JsonQueryException if the query fails, or if it references a variable or function that was
	 * declared without a value and {@link #withRuntimeBindings} did not supply one; other runtime
	 * exceptions and stack overflows during evaluation are wrapped in a {@code JsonQueryException}
	 * @throws RuntimeLimitExceededException if the query exceeds a limit set by {@link #withRuntimeOptions}
	 */
	void apply(JsonNode in, Consumer<? super JsonNode> output) throws JsonQueryException;

	/**
	 * Runs this query against {@code in} and collects every output value into a list.
	 * <p>
	 * Use {@link #apply(Object, Consumer)} instead for a query whose output is large or unbounded: it hands
	 * over each value as it is produced, where this one holds them all until the query is done.
	 * <p>
	 * If the query fails partway through, the values it produced before failing are discarded along with the
	 * list.
	 *
	 * @param in the input JSON node, bound to {@code .}
	 * @return a new list, owned by the caller and safe to modify, holding the output values in the order jq
	 * itself would print them
	 * @throws JsonQueryException if the query fails, or if it references a variable or function that was
	 * declared without a value and {@link #withRuntimeBindings} did not supply one; other runtime
	 * exceptions and stack overflows during evaluation are wrapped in a {@code JsonQueryException}
	 * @throws RuntimeLimitExceededException if the query exceeds a limit set by {@link #withRuntimeOptions}
	 */
	default List<JsonNode> apply(JsonNode in) throws JsonQueryException {
		List<JsonNode> output = new ArrayList<>();
		apply(in, output::add);
		return output;
	}
}
