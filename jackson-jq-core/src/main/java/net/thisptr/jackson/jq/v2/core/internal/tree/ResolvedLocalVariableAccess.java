package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
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
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		if (frame != null) {
			ExecutionStack.PathAndValue<JsonNode> val = frame.getValue(slot);
			if (val != null && val.getValue() != null) {
				output.emit(val.getValue(), requirePath ? val.getPath() : null);
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
