package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ExecutionStack<JsonNode> {
	public List<Object> memory = new ArrayList<>();
	public List<Frame> frames = new ArrayList<>();

	public Frame pushFrame(@Nullable Frame parent, int size) {
		int offset = memory.size();
		for (int i = 0; i < size; ++i)
			memory.add(null);
		Frame frame = new Frame(offset, size, parent);
		frames.add(frame);
		return frame;
	}

	public Frame pushFrame(int size) {
		return pushFrame(null, size);
	}

	public void popFrame() {
		if (frames.isEmpty())
			return;
		Frame frame = frames.remove(frames.size() - 1);
		for (int i = 0; i < frame.size; i++) {
			int idx = frame.offset + i;
			if (idx < memory.size())
				memory.set(idx, null);
		}
		while (!memory.isEmpty() && memory.get(memory.size() - 1) == null) {
			memory.remove(memory.size() - 1);
		}
	}

	public static class PathAndValue<JsonNode> {
		private final @Nullable Path<JsonNode> path;
		private final @Nullable JsonNode value;

		public PathAndValue(@Nullable Path<JsonNode> path, @Nullable JsonNode value) {
			this.value = value;
			this.path = path;
		}

		public @Nullable Path<JsonNode> getPath() {
			return path;
		}

		public @Nullable JsonNode getValue() {
			return value;
		}
	}

	public class Frame {
		private int size;
		private final int offset;
		private final @Nullable Frame parent;
		private @Nullable Closure<JsonNode> closure;

		private Frame(int offset, int size, @Nullable Frame parent) {
			this.offset = offset;
			this.size = size;
			this.parent = parent;
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
			return ExecutionStack.this;
		}

		public int size() {
			return size;
		}

		public int offset() {
			return offset;
		}

		public @Nullable Frame parent() {
			return parent;
		}

		public @Nullable Frame hopParent(int depthDelta) {
			@Var Frame f = this;
			for (int i = 0; i < depthDelta && f != null; i++) {
				f = f.parent;
			}
			return f;
		}

		private @Nullable Object get(int index) {
			if (index < 0 || index >= size)
				return null;
			int realIdx = offset + index;
			if (realIdx >= memory.size())
				return null;
			return memory.get(realIdx);
		}

		private void setRaw(int index, @Nullable Object value) {
			if (index < 0)
				return;
			int realIdx = offset + index;
			while (memory.size() <= realIdx) {
				memory.add(null);
			}
			if (index >= size) {
				size = index + 1;
			}
			memory.set(realIdx, value);
		}

		@SuppressWarnings("unchecked")
		public @Nullable PathAndValue<JsonNode> getValue(int index) {
			Object raw = get(index);
			if (raw instanceof PathAndValue) {
				return (PathAndValue<JsonNode>) raw;
			} else if (raw != null && !(raw instanceof FunctionFactory) && !(raw instanceof Function)) {
				return new PathAndValue<>(null, (JsonNode) raw);
			}
			return null;
		}

		public @Nullable JsonNode getValueNode(int index) {
			PathAndValue<JsonNode> pv = getValue(index);
			return pv != null ? pv.getValue() : null;
		}

		public @Nullable Path<JsonNode> getPath(int index) {
			PathAndValue<JsonNode> pv = getValue(index);
			return pv != null ? pv.getPath() : null;
		}

		@SuppressWarnings("unchecked")
		public @Nullable FunctionFactory getFunctionFactory(int index) {
			Object raw = get(index);
			if (raw instanceof FunctionFactory) {
				return (FunctionFactory) raw;
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		public @Nullable Function<JsonNode> getFunction(int index) {
			Object raw = get(index);
			if (raw instanceof Function) {
				return (Function<JsonNode>) raw;
			}
			return null;
		}

		public void set(int index, @Nullable Path<JsonNode> path, @Nullable JsonNode value) {
			if (path != null) {
				setRaw(index, new PathAndValue<>(path, value));
			} else {
				setRaw(index, value);
			}
		}

		public void set(int index, PathAndValue<JsonNode> value) {
			setRaw(index, value);
		}

		public void set(int index, @Nullable JsonNode value) {
			setRaw(index, value);
		}

		public void set(int index, FunctionFactory factory) {
			setRaw(index, factory);
		}

		public void set(int index, Function<JsonNode> function) {
			setRaw(index, function);
		}
	}
}
