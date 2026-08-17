package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.List;

public class StackMemory {
	// Visible for StackFrame
	final List<Object> memory = new ArrayList<>();

	// @VisibleForTesting
	final List<StackFrame> frames = new ArrayList<>();

	public StackFrame pushFrame(int size) {
		int offset = memory.size();
		for (int i = 0; i < size; ++i)
			memory.add(null);
		StackFrame frame = new StackFrame(this, offset, size);
		frames.add(frame);
		return frame;
	}

	public void popFrame() {
		if (frames.isEmpty())
			throw new IllegalStateException("no stack frame left");
		StackFrame frame = frames.remove(frames.size() - 1);
		// A frame's slots are always exactly the tail of memory (pushFrame only ever appends, frames pop
		// strictly LIFO), so clearing exactly frame.getSize() elements restores memory to the parent frame's
		// boundary precisely -- regardless of whether any of those slots (or the parent's own next-to-last
		// slot) happen to be null. Scanning for trailing nulls instead would risk chewing into a still-active
		// parent frame's own not-yet-written tail slot.
		memory.subList(memory.size() - frame.size(), memory.size()).clear();
	}
}
