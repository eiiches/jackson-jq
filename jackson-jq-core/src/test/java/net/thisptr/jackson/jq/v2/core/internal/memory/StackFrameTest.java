package net.thisptr.jackson.jq.v2.core.internal.memory;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StackFrameTest {

	@Test
	void keepsAnExplicitReferenceToItsExecutionStack() {
		Memory stack = new Memory();

		StackFrame frame = stack.pushFrame(1);

		assertThat(frame.getEnclosingMemory()).isSameAs(stack);
	}

	@Test
	void readsAndWritesFrameSlots() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(3);
		Supplier<String> supplier = () -> "supplied";

		frame.set(0, "value");
		frame.set(2, supplier);

		assertThat(frame.get(0)).isEqualTo("value");
		assertThat(frame.get(1)).isNull();
		assertThat(frame.get(2)).isSameAs(supplier);
	}

	@Test
	void throwsWhenWritingAtOrBeyondFrameSize() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThatThrownBy(() -> frame.set(1, "value")).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void throwsWhenReadingAtOrBeyondFrameSize() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThatThrownBy(() -> frame.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void throwsForNegativeSlotIndices() {
		Memory stack = new Memory();
		StackFrame frame = stack.pushFrame(1);

		assertThatThrownBy(() -> frame.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> frame.set(-1, "value")).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void framesDoNotShareRawValues() {
		Memory stack = new Memory();
		StackFrame parent = stack.pushFrame(1);
		parent.set(0, "parent-value");

		StackFrame child = stack.pushFrame(1);

		assertThat(child.get(0)).isNull();
	}

	@Test
	void poppingAFramePreservesEarlierFrames() {
		Memory stack = new Memory();
		StackFrame parent = stack.pushFrame(1);
		parent.set(0, "parent");
		StackFrame child = stack.pushFrame(2);
		child.set(0, "child");

		stack.popFrame();

		assertThat(parent.get(0)).isEqualTo("parent");
	}

	@Test
	void clearsPoppedSlotsBeforeReusingThem() {
		Memory stack = new Memory();
		StackFrame first = stack.pushFrame(1);
		first.set(0, "value");

		stack.popFrame();
		StackFrame second = stack.pushFrame(1);

		assertThat(second.get(0)).isNull();
	}

	@Test
	void popsNestedZeroSizedFrames() {
		Memory stack = new Memory();
		stack.pushFrame(0);
		stack.pushFrame(0);

		stack.popFrame();
		stack.popFrame();

		assertThatThrownBy(stack::popFrame).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsNegativeFrameSizes() {
		Memory stack = new Memory();

		assertThatThrownBy(() -> stack.pushFrame(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void throwsWhenPoppingAnEmptyStack() {
		Memory stack = new Memory();

		assertThatThrownBy(stack::popFrame).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void readsPreparedGlobalSlots() {
		Memory stack = new Memory(new Object[] { "first", "second" });

		assertThat(stack.getGlobal(0)).isEqualTo("first");
		assertThat(stack.getGlobal(1)).isEqualTo("second");
	}

	@Test
	void defaultsToZeroGlobalSlots() {
		Memory stack = new Memory();

		assertThatThrownBy(() -> stack.getGlobal(0)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void throwsForOutOfBoundsOrNegativeGlobalIndices() {
		Memory stack = new Memory(1);

		assertThatThrownBy(() -> stack.getGlobal(1)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> stack.getGlobal(-1)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void globalsAreIndependentOfFramePushAndPop() {
		Memory stack = new Memory(new Object[] { "global-value" });

		StackFrame frame = stack.pushFrame(1);
		frame.set(0, "frame-value");
		stack.popFrame();

		assertThat(stack.getGlobal(0)).isEqualTo("global-value");
	}
}
