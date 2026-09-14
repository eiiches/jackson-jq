package net.thisptr.jackson.jq.v2.spi;

/**
 * The evaluation state one top-level query invocation carries, handed to every
 * {@link Expression#apply} call as its {@code Context}.
 * <p>
 * The engine's own context type is otherwise opaque to this SPI -- implementations must pass the
 * context they are given straight on when evaluating their arguments, never construct one -- but it
 * always exposes the {@link RuntimeLimits} the invocation was started with, so a {@code Function}
 * that grows an array or object can honour them.
 * <p>
 * It pairs with {@link BindContext} by phase: a {@code BindContext} is handed to a {@link Function}
 * when its call is bound, a {@code RuntimeContext} to the resulting {@link Expression} every time that
 * expression is evaluated.
 */
public interface RuntimeContext {

	/**
	 * Returns the limits this invocation runs under.
	 *
	 * @return the limits, never {@code null}; every limit is unbounded when the caller set none
	 */
	RuntimeLimits getRuntimeLimits();
}
