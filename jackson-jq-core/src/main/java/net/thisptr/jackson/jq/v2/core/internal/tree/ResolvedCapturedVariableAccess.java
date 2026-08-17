package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedCapturedVariableAccess<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final int closureSlot;
	private final int frameClosureSlot;

	public ResolvedCapturedVariableAccess(String name, int closureSlot, int frameClosureSlot) {
		this.name = name;
		this.closureSlot = closureSlot;
		this.frameClosureSlot = frameClosureSlot;
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void apply(@Nullable StackFrame<JsonNode> frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Closure<JsonNode> closure = frame != null ? (Closure<JsonNode>) frame.getRawValue(frameClosureSlot) : null;
		if (closure == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined (no closure)");
		}
		Object raw = closure.getRawValue(closureSlot);
		if (raw == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined");
		}
		if (raw instanceof ExecutionStack.PathAndValue) {
			ExecutionStack.PathAndValue<JsonNode> pv = (ExecutionStack.PathAndValue<JsonNode>) raw;
			if (pv.getValue() != null) {
				output.emit(pv.getValue(), pv.getPath());
			}
		} else {
			output.emit((JsonNode) raw, null);
		}
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
