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
 * <p>
 * {@code Function} instances may be cached and shared by the caller across compilations. Implementations
 * must be safe for concurrent, reentrant calls to {@link #bindArguments}.
 */
public interface Function {

	/**
	 * Binds the given arguments to produce an executable {@link Expression}.
	 * <p>
	 * The returned {@code Expression} may be retained by the caller and evaluated repeatedly, including
	 * concurrently, for the lifetime of the compiled query. It must not carry unsynchronized mutable
	 * per-call state.
	 * <p>
	 * {@code args} is ordered, stable, and unmodifiable. Implementations may keep a reference to it and
	 * read from it later (for example from within the returned {@code Expression}) without defensively
	 * copying it; attempting to mutate it throws {@link UnsupportedOperationException}.
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
