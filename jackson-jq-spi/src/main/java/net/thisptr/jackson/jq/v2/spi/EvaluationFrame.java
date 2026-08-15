package net.thisptr.jackson.jq.v2.spi;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Scope.ValueWithPath;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * An array-backed evaluation frame representing local variable slots allocated at compile time.
 * Provides O(1) indexed variable access without hash map overhead during query evaluation.
 */
public class EvaluationFrame<JsonNode> {
	private final @Nullable EvaluationFrame<JsonNode> parent;
	private final Object[] values;
	private final Path<JsonNode>[] paths;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public EvaluationFrame(@Nullable EvaluationFrame<JsonNode> parent, int slots) {
		this.parent = parent;
		this.values = new Object[slots];
		this.paths = (Path<JsonNode>[]) new Path[slots];
	}

	public @Nullable EvaluationFrame<JsonNode> getParent() {
		return parent;
	}

	@SuppressWarnings("unchecked")
	public @Nullable JsonNode getValue(int slot) {
		if (slot >= 0 && slot < values.length) {
			Object val = values[slot];
			if (val != null)
				return (JsonNode) val;
		}
		if (parent != null)
			return parent.getValue(slot);
		return null;
	}

	public void setValue(int slot, JsonNode value) {
		setValueWithPath(slot, value, null);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path) {
		if (slot >= 0 && slot < values.length) {
			values[slot] = value;
			paths[slot] = path;
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable Path<JsonNode> getPath(int slot) {
		if (slot >= 0 && slot < paths.length) {
			Path<JsonNode> p = paths[slot];
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

	@Override
	public String toString() {
		return "EvaluationFrame[values=" + Arrays.toString(values) + "]";
	}
}
