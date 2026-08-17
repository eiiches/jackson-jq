package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StackFrameTest {

	@Test
	void keepsAnExplicitReferenceToItsExecutionStack() {
		ExecutionStack<String> stack = new ExecutionStack<>();

		StackFrame<String> frame = stack.pushFrame(1);

		assertSame(stack, frame.getStack());
		assertSame(frame, stack.frames.get(0));
	}

	@Test
	void readsAndWritesFrameSlots() {
		ExecutionStack<String> stack = new ExecutionStack<>();
		StackFrame<String> frame = stack.pushFrame(1);
		Supplier<String> supplier = () -> "supplied";

		frame.set(0, "value");
		frame.set(2, supplier);

		assertEquals("value", Objects.requireNonNull(frame.getValue(0)).getValue());
		assertNull(frame.getRawValue(1));
		assertSame(supplier, frame.getValueSupplier(2));
		assertNull(frame.getRawValue(-1));
	}

	@Test
	void setRawValueRoundTripsArbitraryObjects() {
		ExecutionStack<String> stack = new ExecutionStack<>();
		StackFrame<String> frame = stack.pushFrame(1);
		Closure<String> closure = new Closure<>(0);

		frame.setRawValue(0, closure);

		assertSame(closure, frame.getRawValue(0));
	}

	@Test
	void framesDoNotShareRawValues() {
		ExecutionStack<String> stack = new ExecutionStack<>();
		StackFrame<String> parent = stack.pushFrame(1);
		parent.setRawValue(0, "parent-value");

		StackFrame<String> child = stack.pushFrame(1);

		assertNull(child.getRawValue(0));
	}

	@Test
	void poppingAFramePreservesEarlierFrames() {
		ExecutionStack<String> stack = new ExecutionStack<>();
		StackFrame<String> parent = stack.pushFrame(1);
		parent.set(0, "parent");
		StackFrame<String> child = stack.pushFrame(2);
		child.set(0, "child");

		stack.popFrame();

		assertEquals(1, stack.frames.size());
		assertSame(parent, stack.frames.get(0));
		assertEquals(1, stack.memory.size());
		assertEquals("parent", Objects.requireNonNull(parent.getValue(0)).getValue());
	}
}
