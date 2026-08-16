package net.thisptr.jackson.jq.v2.spi;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A contiguous array-backed execution stack for query evaluation.
 * Represents stack memory with a Stack Pointer (sp) and supports O(1) indexed
 * variable access relative to Base Pointers (bp).
 */
public class EvaluationStack<JsonNode> {
	private Object[] values;
	private Path<JsonNode>[] paths;
	private int sp = 0; // Stack Pointer (top of stack)

	@SuppressWarnings({"unchecked", "rawtypes"})
	public EvaluationStack(int initialCapacity) {
		this.values = new Object[initialCapacity];
		this.paths = (Path<JsonNode>[]) new Path[initialCapacity];
	}

	public EvaluationStack() {
		this(256);
	}

	public int sp() {
		return sp;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void ensureCapacity(int minCapacity) {
		if (minCapacity > values.length) {
			int newCap = Math.max(values.length * 2, minCapacity);
			values = Arrays.copyOf(values, newCap);
			paths = Arrays.copyOf(paths, newCap);
		}
	}

	public int pushFrame(int size) {
		int bp = sp;
		ensureCapacity(sp + size);
		sp += size;
		return bp;
	}

	public void popFrame(int bp) {
		if (bp >= 0 && bp < sp) {
			Arrays.fill(values, bp, sp, null);
			Arrays.fill(paths, bp, sp, null);
			sp = bp;
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable JsonNode getValue(int index) {
		if (index >= 0 && index < sp) {
			Object val = values[index];
			if (val != null)
				return (JsonNode) val;
		}
		return null;
	}

	public @Nullable Path<JsonNode> getPath(int index) {
		if (index >= 0 && index < sp) {
			Path<JsonNode> p = paths[index];
			if (p != null)
				return p;
		}
		return null;
	}

	public void setValueWithPath(int index, JsonNode value, @Nullable Path<JsonNode> path) {
		if (index >= 0) {
			ensureCapacity(index + 1);
			values[index] = value;
			paths[index] = path;
			if (index >= sp)
				sp = index + 1;
		}
	}

	public void setValue(int index, JsonNode value) {
		setValueWithPath(index, value, null);
	}
}
