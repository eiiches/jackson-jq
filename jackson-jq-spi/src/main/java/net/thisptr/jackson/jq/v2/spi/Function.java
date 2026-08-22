package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Factory for creating executable {@link Expression} instances.
 * <p>
 * A {@code Function} represents a compiled function expression bound to its lexical environment.
 * Any captured lexical state (such as a {@code Closure}) is encapsulated internally by the specific
 * {@code Function} implementation rather than supplied dynamically at invocation time by callers.
 */
public interface Function {

	/**
	 * Binds the given arguments to produce an executable {@link Expression}.
	 *
	 * @param <JsonNode>   the JSON node type
	 * @param <Context>    an opaque object representing execution state that has to be passed on when evaluating function arguments
	 * @param jsonProvider the JSON provider
	 * @param args         the argument expressions to bind
	 * @param jqVersion    the jq compatibility version
	 * @return the bound expression
	 */
	<Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version jqVersion) throws JsonQueryException;
}
