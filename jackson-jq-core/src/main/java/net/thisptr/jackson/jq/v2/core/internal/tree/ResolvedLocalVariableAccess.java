package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.EvaluationFrame;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Scope.ValueWithPath;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalVariableAccess implements Expression {
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
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		EvaluationFrame<JsonNode> frame = scope.getEvaluationFrame();
		if (frame != null) {
			ValueWithPath<JsonNode> val = frame.getValueWithPath(slot);
			if (val != null) {
				output.emit(val.value(), requirePath ? val.path() : null);
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
