package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Factory for creating executable {@link Expression} instances.
 * <p>
 * A {@code Function} represents a compiled function expression bound to its lexical environment.
 * Any captured lexical state (such as a {@code Closure}) is encapsulated internally by the specific
 * {@code Function} implementation rather than supplied dynamically at invocation time by callers.
 * <p>
 * {@code Function} instances may be cached and shared by the caller across compilations. Implementations
 * must be safe for concurrent, reentrant calls to {@link #bind}.
 * <p>
 * This SPI does not specify which registration wins if multiple providers (or multiple
 * {@code @FunctionRegistration}s) resolve to the same {@code FunctionSignature}; which one is used
 * is left to the function/module loader in use (e.g. {@code ClassPathFunctionLoader} picks one
 * arbitrarily, based on {@link java.util.ServiceLoader} discovery order).
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
	 * <p>
	 * Everything else the engine supplies arrives through {@code bindCtx} rather than as a parameter of its
	 * own, so that a later release can hand implementations more without changing this signature. An
	 * implementation that uses the {@link BindContext#getJsonProvider() provider} or the
	 * {@link BindContext#getJqVersion() jq version} from within the returned {@code Expression} should
	 * read them into local variables here and capture those, rather than capturing {@code bindCtx} and
	 * dereferencing it on every evaluation.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param <Context> the execution state that has to be passed on when evaluating function arguments; opaque
	 * apart from the {@link RuntimeContext#getRuntimeLimits() limits} it exposes
	 * @param bindCtx the compilation state this call is bound in
	 * @param args the argument expressions to bind
	 * @return the bound expression
	 */
	<Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) throws JsonQueryException;
}
