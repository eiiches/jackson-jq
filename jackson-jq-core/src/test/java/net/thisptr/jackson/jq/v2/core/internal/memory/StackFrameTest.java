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

		assertEquals("parent", parent.get(0));
	}

	@Test
	void clearsPoppedSlotsBeforeReusingThem() {
		Memory stack = new Memory();
		StackFrame first = stack.pushFrame(1);
		first.set(0, "value");

		stack.popFrame();
		StackFrame second = stack.pushFrame(1);

		assertNull(second.get(0));
	}

	@Test
	void popsNestedZeroSizedFrames() {
		Memory stack = new Memory();
		stack.pushFrame(0);
		stack.pushFrame(0);

		stack.popFrame();
		stack.popFrame();

		assertThrows(IllegalStateException.class, stack::popFrame);
	}

	@Test
	void rejectsNegativeFrameSizes() {
		Memory stack = new Memory();

		assertThrows(IllegalArgumentException.class, () -> stack.pushFrame(-1));
	}

	@Test
	void throwsWhenPoppingAnEmptyStack() {
		Memory stack = new Memory();

		assertThrows(IllegalStateException.class, stack::popFrame);
	}

	@Test
	void readsPreparedGlobalSlots() {
		Memory stack = new Memory(new Object[] { "first", "second" });

		assertEquals("first", stack.getGlobal(0));
		assertEquals("second", stack.getGlobal(1));
	}

	@Test
	void defaultsToZeroGlobalSlots() {
		Memory stack = new Memory();

		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(0));
	}

	@Test
	void throwsForOutOfBoundsOrNegativeGlobalIndices() {
		Memory stack = new Memory(1);

		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(1));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.getGlobal(-1));
	}

	@Test
	void globalsAreIndependentOfFramePushAndPop() {
		Memory stack = new Memory(new Object[] { "global-value" });

		StackFrame frame = stack.pushFrame(1);
		frame.set(0, "frame-value");
		stack.popFrame();

		assertEquals("global-value", stack.getGlobal(0));
	}
}
