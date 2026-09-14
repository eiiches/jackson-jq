package net.thisptr.jackson.jq.v2.core.internal.memory;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StackFrameTest {

	@Test
	void keepsAnExplicitReferenceToItsExecutionStack() {
		Memory stack = new Memory();

		StackFrame frame = stack.pushFrame(1);

		assertSame(stack, frame.getEnclosingMemory());
		assertSame(frame, stack.frames.get(0));
	}

	@Test
	void readsAndWritesFrameSlots() {
		Memory stack = new Memory();
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
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.set(1, "value"));
	}

	@Test
	void throwsWhenReadingAtOrBeyondFrameSize() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.get(1));
	}

	@Test
	void throwsForNegativeSlotIndices() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThrows(IndexOutOfBoundsException.class, () -> frame.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> frame.set(-1, "value"));
	}

	@Test
	void framesDoNotShareRawValues() {
		Memory stack = new Memory();
		StackFrame parent = stack.pushFrame(1);
		parent.set(0, "parent-value");

		StackFrame child = stack.pushFrame(1);

		assertNull(child.get(0));
	}

	@Test
	void poppingAFramePreservesEarlierFrames() {
		Memory stack = new Memory();
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

	@Test
	void readsAndWritesGlobalSlots() {
		Memory stack = new Memory(2);

		stack.setGlobal(0, "first");
		stack.setGlobal(1, "second");

		assertEquals("first", stack.getGlobal(0));
		assertEquals("second", stack.getGlobal(1));
	}

	@Test
	void defaultsToZeroGlobalSlots() {
		Memory stack = new Memory();

		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(0));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.setGlobal(0, "value"));
	}

	@Test
	void throwsForOutOfBoundsOrNegativeGlobalIndices() {
		Memory stack = new Memory(1);

		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(1));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.setGlobal(1, "value"));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.setGlobal(-1, "value"));
	}

	@Test
	void globalsAreIndependentOfFramePushAndPop() {
		Memory stack = new Memory(1);
		stack.setGlobal(0, "global-value");

		StackFrame frame = stack.pushFrame(1);
		frame.set(0, "frame-value");
		stack.popFrame();

		assertEquals("global-value", stack.getGlobal(0));
	}
}
