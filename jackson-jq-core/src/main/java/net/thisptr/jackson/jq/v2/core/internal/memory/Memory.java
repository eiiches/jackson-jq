package net.thisptr.jackson.jq.v2.core.internal.memory;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;

/**
 * Stores global values and stack frames for one query execution.
 *
 * <p>Globals live in a separate, fixed-size array. Local frame values share the growable {@link #slots} array, whose
 * active region has the following layout:
 *
 * <pre>
 * [parent frame][frame slots...][parent frame][frame slots...] ...
 * ^ frame start  ^ StackFrame offset
 * </pre>
 *
 * <p>Each frame starts with a hidden reference to its parent, followed by the slots addressable through
 * {@link StackFrame#get(int)} and {@link StackFrame#set(int, Object)}. {@link #topFrame} identifies the frame to pop,
 * while {@link #usedSlots} marks the end of the active region. Popping a frame clears its parent link and values before
 * moving both markers back to the parent boundary, so the unused capacity can be reused without retaining references.
 */
public class Memory {
	private static final Object[] EMPTY_SLOTS = new Object[0];
	private static final int MINIMUM_GROWN_CAPACITY = 16;

	// Visible for StackFrame
	Object[] slots = EMPTY_SLOTS;

	private int usedSlots;
	private @Nullable StackFrame topFrame;

	// Flat, frame-independent storage for declared-global values. A prepared array is shared by every
	// invocation of one immutable query view and is never written during execution. Unlike the active frame
	// region, it never grows/shrinks with pushFrame/popFrame. Reachable from any frame at any def-nesting depth via
	// StackFrame#getEnclosingMemory().
	private final Object[] globals;

	// The budgets this top-level apply() runs under -- frame-independent, like `globals`, and reachable
	// from any frame via StackFrame#getRuntimeLimits(), which is how Expression/Function implementations
	// (including third-party ones) see them. Declared as the engine's own RuntimeLimitsImpl rather than the
	// SPI interface because the user-defined function call budget is deliberately not on the interface.
	private final RuntimeLimitsImpl runtimeLimits;

	// Read out of runtimeLimits once so counting a call is a plain field compare rather than a virtual call.
	private final long maxUserDefinedFunctionCalls;

	// How many query-text `def` bodies this one top-level apply() has run so far. Unlike `globals` and
	// `runtimeLimits` this accumulates, which is why it belongs here and not on the (immutable, shared)
	// limits: a Memory is built fresh per invocation, so every invocation starts the budget over and
	// concurrent invocations of one compiled query never see each other's tally.
	private long userDefinedFunctionCalls;

	public Memory() {
		this(0);
	}

	public Memory(int globalCount) {
		this(globalCount, RuntimeLimitsImpl.UNLIMITED);
	}

	public Memory(int globalCount, RuntimeLimitsImpl runtimeLimits) {
		this(new Object[globalCount], runtimeLimits);
	}

	public Memory(Object[] globals) {
		this(globals, RuntimeLimitsImpl.UNLIMITED);
	}

	public Memory(Object[] globals, RuntimeLimitsImpl runtimeLimits) {
		this.globals = globals;
		this.runtimeLimits = runtimeLimits;
		this.maxUserDefinedFunctionCalls = runtimeLimits.getMaxUserDefinedFunctionCalls();
	}

	public RuntimeLimits getRuntimeLimits() {
		return runtimeLimits;
	}

	/**
	 * Charges one execution of a query-text {@code def} body against this invocation's budget.
	 * <p>
	 * Called only from {@code ResolvedFunctionDefinition}, and only for a {@code def} the compiler marked as
	 * coming from the query text -- builtins, jq-library bodies and imported modules compile to the same node
	 * but are not metered.
	 *
	 * @throws net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException if the budget is exhausted
	 */
	public void countUserDefinedFunctionCall() {
		if (++userDefinedFunctionCalls > maxUserDefinedFunctionCalls)
			throw RuntimeLimitChecks.userDefinedFunctionCallsExceeded(maxUserDefinedFunctionCalls);
	}

	public @Nullable Object getGlobal(int index) {
		if (index < 0 || index >= globals.length)
			throw new IndexOutOfBoundsException("global " + index + " out of bounds for global count " + globals.length);
		return globals[index];
	}

	public StackFrame pushFrame(int size) {
		if (size < 0)
			throw new IllegalArgumentException("frame size must not be negative: " + size);
		if (size >= Integer.MAX_VALUE - usedSlots)
			throw new OutOfMemoryError("required frame storage is too large");

		int frameStart = usedSlots;
		int requiredCapacity = frameStart + size + 1;
		ensureCapacity(requiredCapacity);
		// Store the link outside the frame's addressable slots so StackFrame stays compact.
		slots[frameStart] = topFrame;
		usedSlots = requiredCapacity;

		StackFrame frame = new StackFrame(this, frameStart + 1, size);
		topFrame = frame;
		return frame;
	}

	public void popFrame() {
		StackFrame frame = topFrame;
		if (frame == null)
			throw new IllegalStateException("no stack frame left");

		int frameStart = frame.offset() - 1;
		StackFrame parent = (StackFrame) slots[frameStart];
		Arrays.fill(slots, frameStart, usedSlots, null);
		usedSlots = frameStart;
		topFrame = parent;
	}

	private void ensureCapacity(int requiredCapacity) {
		if (requiredCapacity <= slots.length)
			return;

		// Keep a lone root frame exact-sized, but avoid repeated small reallocations once frames nest.
		int preferredCapacity = slots.length == 0 ? requiredCapacity
				: Math.max(MINIMUM_GROWN_CAPACITY, slots.length + (slots.length >> 1));
		int newCapacity = Math.max(requiredCapacity, preferredCapacity);
		slots = Arrays.copyOf(slots, newCapacity);
	}
}
