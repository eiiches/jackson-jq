package net.thisptr.jackson.jq.v2.spi;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StackFrameTest {

	@Test
	void keepsAnExplicitReferenceToItsExecutionStack() {
		StackMemory stack = new StackMemory();

		StackFrame frame = stack.pushFrame(1);

		assertSame(stack, frame.getEnclosingMemory());
		assertSame(frame, stack.frames.get(0));
	}

	@Test
	void readsAndWritesFrameSlots() {
		StackMemory stack = new StackMemory();
		StackFrame frame = stack.pushFrame(3);
		Supplier<String> supplier = () -> "supplied";

		frame.set(0, "value");
		frame.set(2, supplier);

		assertEquals("value", frame.get(0));
		assertNull(frame.get(1));
		assertSame(supplier, frame.get(2));
	}

	@Test
	void throwsWhenWritingAtOrBeyondFrameSize() {
		StackMemory stack = new StackMemory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.set(1, "value"));
	}

	@Test
	void throwsWhenReadingAtOrBeyondFrameSize() {
		StackMemory stack = new StackMemory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.get(1));
	}

	@Test
	void throwsForNegativeSlotIndices() {
		StackMemory stack = new StackMemory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> frame.set(-1, "value"));
	}

	@Test
	void framesDoNotShareRawValues() {
		StackMemory stack = new StackMemory();
		StackFrame parent = stack.pushFrame(1);
		parent.set(0, "parent-value");

		StackFrame child = stack.pushFrame(1);

		assertNull(child.get(0));
	}

	@Test
	void poppingAFramePreservesEarlierFrames() {
		StackMemory stack = new StackMemory();
		StackFrame parent = stack.pushFrame(1);
		parent.set(0, "parent");
		StackFrame child = stack.pushFrame(2);
		child.set(0, "child");

		stack.popFrame();

		assertEquals(1, stack.frames.size());
		assertSame(parent, stack.frames.get(0));
		assertEquals(1, stack.memory.size());
		assertEquals("parent", parent.get(0));
	}
}
