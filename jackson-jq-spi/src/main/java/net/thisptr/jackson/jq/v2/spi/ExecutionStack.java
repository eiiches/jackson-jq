package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ExecutionStack<JsonNode> {
	public List<Object> memory = new ArrayList<>();
	public List<StackFrame<JsonNode>> frames = new ArrayList<>();

	public StackFrame<JsonNode> pushFrame(@Nullable StackFrame<JsonNode> parent, int size) {
		int offset = memory.size();
		for (int i = 0; i < size; ++i)
			memory.add(null);
		StackFrame<JsonNode> frame = new StackFrame<>(this, offset, size, parent);
		frames.add(frame);
		return frame;
	}

	public void popFrame() {
		if (frames.isEmpty())
			return;
		StackFrame<JsonNode> frame = frames.remove(frames.size() - 1);
		for (int i = 0; i < frame.getSize(); i++) {
			int idx = frame.getOffset() + i;
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
}
