package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalVariableAccess<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final int slot;

	public ResolvedLocalVariableAccess(String name, int slot) {
		this.name = name;
		this.slot = slot;
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		if (frame != null) {
			PathAndValue<JsonNode> val = StackFrameValues.asPathAndValue(frame.get(slot));
			if (val != null && val.getValue() != null) {
				output.emit(val.getValue(), path != null ? val.getPath() : null);
				return;
			}
		}
		throw new JsonQueryException(String.format("$%s is not defined", name));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
