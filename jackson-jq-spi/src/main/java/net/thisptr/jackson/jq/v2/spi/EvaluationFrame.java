package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Scope.ValueWithPath;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Represents a stack frame offset (defined by Base Pointer 'bp' and size)
 * over a contiguous EvaluationStack.
 */
public class EvaluationFrame<JsonNode> {
	private final EvaluationStack<JsonNode> stack;
	private final int bp; // Base Pointer offset
	private final @Nullable EvaluationFrame<JsonNode> parent; // Static link / lexical parent scope
	private int size;

	public EvaluationFrame(EvaluationStack<JsonNode> stack, int bp, @Nullable EvaluationFrame<JsonNode> parent, int size) {
		this.stack = stack;
		this.bp = bp;
		this.parent = parent;
		this.size = size;
	}

	public EvaluationFrame(@Nullable EvaluationFrame<JsonNode> parent, int size) {
		this(parent != null ? parent.getStack() : new EvaluationStack<>(), parent != null ? parent.getStack().pushFrame(size) : 0, parent, size);
	}

	public EvaluationStack<JsonNode> getStack() {
		return stack;
	}

	public int bp() {
		return bp;
	}

	public int size() {
		return size;
	}

	public @Nullable EvaluationFrame<JsonNode> getParent() {
		return parent;
	}

	public @Nullable JsonNode getValue(int slot) {
		if (slot >= 0) {
			JsonNode val = stack.getValue(bp + slot);
			if (val != null)
				return val;
		}
		if (parent != null)
			return parent.getValue(slot);
		return null;
	}

	public void setValue(int slot, JsonNode value) {
		setValueWithPath(slot, value, null);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path) {
		if (slot >= 0) {
			stack.setValueWithPath(bp + slot, value, path);
			if (slot >= size)
				size = slot + 1;
		}
	}

	public @Nullable Path<JsonNode> getPath(int slot) {
		if (slot >= 0) {
			Path<JsonNode> p = stack.getPath(bp + slot);
			if (p != null)
				return p;
		}
		if (parent != null)
			return parent.getPath(slot);
		return null;
	}

	public @Nullable ValueWithPath<JsonNode> getValueWithPath(int slot) {
		JsonNode val = getValue(slot);
		if (val == null)
			return null;
		Path<JsonNode> p = getPath(slot);
		return new ValueWithPath<JsonNode>() {
			@Override
			public JsonNode value() {
				return val;
			}

			@Override
			public @Nullable Path<JsonNode> path() {
				return p;
			}
		};
	}

	public void pop() {
		stack.popFrame(bp);
	}

	@Override
	public String toString() {
		return "EvaluationFrame[bp=" + bp + ", size=" + size + "]";
	}
}
