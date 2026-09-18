package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;

/**
 * How a {@link ResolvedTailCall} buried in a {@code def} body tells the loop at the activation boundary to
 * go round again.
 * <p>
 * It lives in a slot of the frame the body is running in, put there by that loop and read back out by the
 * call site -- the same channel a {@link Closure} rides on, and for the same reason: the {@code frame}
 * parameter of {@code Expression#apply} is the only thing every node in a body receives, however deep. A
 * slot holding {@code null} means no loop is running and the call has to be made the ordinary way.
 * <p>
 * {@link StackFrame} knows nothing of this, exactly as it knows nothing of closures. That a slot may hold
 * one of these is a convention between this class, {@link ResolvedFunctionDefinition} and
 * {@link ResolvedTailCall}, which is why all three live together.
 */
public final class TailCallJump {
	static final Object[] NO_ARGUMENTS = new Object[0];

	// The body currently running, so a call site can tell a call to the def it is already in -- which needs
	// no new frame, only new values in the parameter slots it already has -- from a call to another def.
	private @Nullable TailCallTarget owner;

	private @Nullable TailCallTarget target;
	private Object[] arguments = NO_ARGUMENTS;
	private @Nullable Object input;
	private @Nullable Object ipath;
	private @Nullable Object output;

	void arm(TailCallTarget owner) {
		this.owner = owner;
	}

	/**
	 * The body currently running.
	 *
	 * @return the target the loop is on
	 */
	public TailCallTarget owner() {
		TailCallTarget running = owner;
		if (running == null)
			throw new IllegalStateException("no body is running");
		return running;
	}

	/**
	 * Asks the loop to run {@code target} next.
	 *
	 * @param target the body to run
	 * @param arguments its parameters, or empty when the call site has already written them into the frame
	 * because {@code target} is the body it was running in
	 * @param input the callee's input; may be Java {@code null}, which is how some providers spell JSON
	 * {@code null} -- {@link #isPending()} keys off {@code target}, never off this
	 * @param ipath the callee's input path
	 * @param output where the callee's values go
	 */
	public void jumpTo(TailCallTarget target, Object[] arguments, @Nullable Object input, Object ipath, Object output) {
		this.target = target;
		this.arguments = arguments;
		this.input = input;
		this.ipath = ipath;
		this.output = output;
	}

	boolean isPending() {
		return target != null;
	}

	TailCallTarget target() {
		TailCallTarget pending = target;
		if (pending == null)
			throw new IllegalStateException("no tail call is pending");
		return pending;
	}

	Object[] arguments() {
		return arguments;
	}

	@Nullable Object input() {
		return input;
	}

	Object ipath() {
		Object pending = ipath;
		if (pending == null)
			throw new IllegalStateException("no tail call is pending");
		return pending;
	}

	Object output() {
		Object pending = output;
		if (pending == null)
			throw new IllegalStateException("no tail call is pending");
		return pending;
	}

	void clear() {
		target = null;
		arguments = NO_ARGUMENTS;
		input = null;
		ipath = null;
		output = null;
	}
}
