package net.thisptr.jackson.jq.v2.spi;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StackFrame<JsonNode> {
	private final ExecutionStack<JsonNode> stack;
	private int size;
	private final int offset;
	private final @Nullable StackFrame<JsonNode> parent;
	private @Nullable Closure<JsonNode> closure;

	StackFrame(ExecutionStack<JsonNode> stack, int offset, int size, @Nullable StackFrame<JsonNode> parent) {
		this.stack = stack;
		this.offset = offset;
		this.size = size;
		this.parent = parent;
	}

	int getSize() {
		return size;
	}

	int getOffset() {
		return offset;
	}

	public @Nullable Closure<JsonNode> getClosure() {
		if (closure != null)
			return closure;
		if (parent != null)
			return parent.getClosure();
		return null;
	}

	public void setClosure(@Nullable Closure<JsonNode> closure) {
		this.closure = closure;
	}

	public ExecutionStack<JsonNode> getStack() {
		return stack;
	}

	private @Nullable Object get(int index) {
		if (index < 0 || index >= size)
			return null;
		int realIdx = offset + index;
		List<Object> memory = stack.memory;
		if (realIdx >= memory.size())
			return null;
		return memory.get(realIdx);
	}

	public @Nullable Object getRawValue(int index) {
		return get(index);
	}

	private void setRaw(int index, @Nullable Object value) {
		if (index < 0)
			return;
		int realIdx = offset + index;
		List<Object> memory = stack.memory;
		while (memory.size() <= realIdx) {
			memory.add(null);
		}
		if (index >= size) {
			size = index + 1;
		}
		memory.set(realIdx, value);
	}

	@SuppressWarnings("unchecked")
	public ExecutionStack.@Nullable PathAndValue<JsonNode> getValue(int index) {
		Object raw = get(index);
		if (raw instanceof ExecutionStack.PathAndValue) {
			return (ExecutionStack.PathAndValue<JsonNode>) raw;
		} else if (raw != null && !(raw instanceof FunctionFactory) && !(raw instanceof Function)) {
			return new ExecutionStack.PathAndValue<>(null, (JsonNode) raw);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public @Nullable Supplier<JsonNode> getValueSupplier(int index) {
		Object raw = get(index);
		return raw instanceof Supplier ? (Supplier<JsonNode>) raw : null;
	}

	public @Nullable FunctionFactory getFunctionFactory(int index) {
		Object raw = get(index);
		if (raw instanceof FunctionFactory) {
			return (FunctionFactory) raw;
		}
		return null;
	}

	public void set(int index, @Nullable Path<JsonNode> path, @Nullable JsonNode value) {
		if (path != null) {
			setRaw(index, new ExecutionStack.PathAndValue<>(path, value));
		} else {
			setRaw(index, value);
		}
	}

	public void set(int index, @Nullable JsonNode value) {
		setRaw(index, value);
	}

	public void set(int index, Supplier<JsonNode> supplier) {
		setRaw(index, supplier);
	}

	public void set(int index, FunctionFactory factory) {
		setRaw(index, factory);
	}
}
