package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * One jq {@code def} body, as the loop in {@link ResolvedFunctionDefinition} drives it.
 * <p>
 * An ordinary call costs a chain of Java frames -- the call site, {@code Function#bind}, the bound lambda,
 * parameter binding, the body -- and none of them can be reclaimed until the callee returns, because the
 * callee's values are pushed through an {@code Output} the caller supplied. A tail call instead runs the
 * next body from the same Java frame, through these four operations.
 * <p>
 * Nothing here pushes, pops or loops. The frame belongs to the one loop draining the chain, which is what
 * keeps a mutual {@code a -> b -> a} recursion flat instead of growing a Java frame per hop.
 */
public interface TailCallTarget {
	/**
	 * Stands in for the frame slot of a body holding no tail call, which therefore needs no loop.
	 */
	int NO_TAIL_CALL = -1;

	/**
	 * The size of the frame this body runs in.
	 *
	 * @return the slot count, as {@code CompileContext#getSlotCount} reported it
	 */
	int frameSize();

	/**
	 * The slot in that frame where this body's tail calls look for their {@link TailCallJump}.
	 *
	 * @return the slot, or {@link #NO_TAIL_CALL}
	 */
	int tailCallSlot();

	/**
	 * Fills a freshly pushed frame with what this body needs to run: its own {@code Closure}, and one value
	 * per declared parameter.
	 *
	 * @param frame the frame, of {@link #frameSize()} slots
	 * @param arguments one entry per declared parameter, in declaration order -- a value for a {@code $}
	 * parameter, a {@link net.thisptr.jackson.jq.v2.spi.Function} for a filter parameter
	 */
	void install(StackFrame frame, Object[] arguments);

	/**
	 * Runs this body exactly once against {@code frame}, charging the call budget if this {@code def} is
	 * metered.
	 *
	 * @param frame the frame {@link #install} prepared
	 * @param in the input JSON node, Java {@code null} when the provider spells JSON {@code null} that way
	 * @param ipath the input's {@code Path}
	 * @param output where this body's values go
	 * @throws JsonQueryException if evaluating the body does
	 */
	void runBody(StackFrame frame, @Nullable Object in, Object ipath, Object output) throws JsonQueryException;
}
