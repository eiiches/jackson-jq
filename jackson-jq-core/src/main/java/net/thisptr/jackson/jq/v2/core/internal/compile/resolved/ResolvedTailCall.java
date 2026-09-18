package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.AbstractDelegatingExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A call the compiler found in tail position, which updates the callee's arguments and asks the enclosing
 * {@code def}'s loop to run it, rather than calling it here.
 * <p>
 * Evaluating this emits nothing and returns at once. What makes that sound is where the compiler is willing
 * to put one: in tail position, and only where everything still in progress to its left emits at most one
 * value (see {@code Compiler}'s tail-position rules, and the jq manual's own statement of the same
 * condition). The Java frames between here and the loop therefore have no work left, and unwinding them
 * loses nothing -- which is exactly what an ordinary call cannot do, and why a few hundred iterations
 * exhaust the stack without this.
 * <p>
 * A call back into the {@code def} this node sits in is the common case, and the cheap one: that frame
 * already has the right shape, so the new arguments go straight into the parameter slots and nothing is
 * pushed, popped or allocated. A call to another {@code def} hands its arguments to the loop, which
 * replaces the frame.
 * <p>
 * It wraps the ordinary call rather than replacing it, both to answer the compiler's questions with the same
 * answers ({@link AbstractDelegatingExpression}) and because two runtime conditions fall back to it: no loop
 * running, and a callee that is not a {@link TailCallTarget}.
 *
 * @param <JsonNode> the JSON node type
 */
public final class ResolvedTailCall<JsonNode> extends AbstractDelegatingExpression<JsonNode> {
	/**
	 * Says a function lives in a frame slot rather than in the current function's own {@code Closure}.
	 */
	public static final int LOCAL = -1;

	private final int tailCallSlot;
	private final int frameClosureSlot;
	private final int slot;
	private final List<TailCallArgument<JsonNode>> arguments;
	// The enclosing def's own parameter slots, present only when this call's signature is the enclosing def's
	// -- the necessary condition for a call back into it. Null means the callee is certainly another def.
	private final int @Nullable [] selfParameterSlots;

	public ResolvedTailCall(Expression<StackFrame, JsonNode> ordinaryCall, int tailCallSlot, int frameClosureSlot, int slot, List<TailCallArgument<JsonNode>> arguments, int @Nullable [] selfParameterSlots) {
		super(ordinaryCall);
		this.tailCallSlot = tailCallSlot;
		this.frameClosureSlot = frameClosureSlot;
		this.slot = slot;
		this.arguments = arguments;
		this.selfParameterSlots = selfParameterSlots;
	}

	@Override
	protected Expression<StackFrame, JsonNode> recreate(Expression<StackFrame, JsonNode> rewrittenInner) {
		// A rewritten call site is a different call site, and the argument resolvers here were built from the
		// old one. Give back the plain call: the only thing lost is the optimization, and a def body is a fold
		// barrier (CompileContext#markFoldBarrier), so nothing rewrites one today.
		return rewrittenInner;
	}

	/**
	 * Reads a {@link Function} out of a frame slot, or out of the current function's own {@code Closure}.
	 *
	 * @param frame the current frame
	 * @param frameClosureSlot the slot holding this function's {@code Closure}, or {@link #LOCAL}
	 * @param slot the slot within the frame, or within the closure
	 * @return the function, or {@code null} if nothing is bound there
	 */
	static @Nullable Function readFunction(StackFrame frame, int frameClosureSlot, int slot) {
		if (frameClosureSlot == LOCAL)
			return (Function) frame.get(slot);
		Closure closure = (Closure) frame.get(frameClosureSlot);
		return closure == null ? null : (Function) closure.get(slot);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		// Null whenever this node is reached outside the loop that would run it -- ConstantFolder evaluating a
		// compiled subtree on its own, say. Structurally that should not happen; it costs one slot read to not
		// depend on that.
		TailCallJump jump = (TailCallJump) frame.get(tailCallSlot);
		if (jump == null) {
			inner.apply(frame, in, ipath, output);
			return;
		}
		Function callee = readFunction(frame, frameClosureSlot, slot);
		if (!(callee instanceof TailCallTarget)) {
			inner.apply(frame, in, ipath, output);
			return;
		}
		Object[] resolved = arguments.isEmpty() ? TailCallJump.NO_ARGUMENTS : new Object[arguments.size()];
		for (int i = 0; i < resolved.length; i++) {
			Object argument = arguments.get(i).resolve(frame, in, ipath);
			if (argument == TailCallArgument.NO_VALUE)
				return;
			if (argument == TailCallArgument.FALL_BACK) {
				inner.apply(frame, in, ipath, output);
				return;
			}
			resolved[i] = argument;
		}
		if (selfParameterSlots != null && callee == jump.owner()) {
			// All of them were evaluated above before any was stored, because an argument reads the parameters
			// it is about to replace: `f($n - 1; $n - 2)` means the caller's $n in both.
			for (int i = 0; i < resolved.length; i++)
				frame.set(selfParameterSlots[i], resolved[i]);
			jump.jumpTo((TailCallTarget) callee, TailCallJump.NO_ARGUMENTS, in, ipath, output);
			return;
		}
		jump.jumpTo((TailCallTarget) callee, resolved, in, ipath, output);
	}
}
