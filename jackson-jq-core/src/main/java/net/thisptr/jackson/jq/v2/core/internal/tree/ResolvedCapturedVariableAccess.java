package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedCapturedVariableAccess implements Expression {
	private final String name;
	private final int closureSlot;

	public ResolvedCapturedVariableAccess(String name, int closureSlot) {
		this.name = name;
		this.closureSlot = closureSlot;
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		ExecutionStack<JsonNode>.Frame frame = scope.getExecutionFrame();
		Closure<JsonNode> closure = frame != null ? frame.getClosure() : null;
		if (closure == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined (no closure)");
		}
		Object raw = closure.getVariable(closureSlot);
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
