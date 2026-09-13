package net.thisptr.jackson.jq.v2.core.internal.memory;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;

public class StackFrame implements RuntimeContext {
	private final Memory stack;
	private final int size;
	private final int offset;

	StackFrame(Memory stack, int offset, int size) {
		this.stack = stack;
		this.offset = offset;
		this.size = size;
	}

	public int size() {
		return size;
	}

	public Memory getEnclosingMemory() {
		return stack;
	}

	@Override
	public RuntimeLimits getRuntimeLimits() {
		return stack.getRuntimeLimits();
	}

	public @Nullable Object get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("slot " + index + " out of bounds for frame size " + size);
		return stack.memory.get(offset + index);
	}

	public void set(int index, @Nullable Object value) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("slot " + index + " out of bounds for frame size " + size);
		stack.memory.set(offset + index, value);
	}
}
