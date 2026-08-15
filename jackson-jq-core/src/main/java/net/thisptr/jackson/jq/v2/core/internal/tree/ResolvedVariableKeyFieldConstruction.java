package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.EvaluationFrame;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class ResolvedVariableKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final String name;
	private final int slot;

	public ResolvedVariableKeyFieldConstruction(String name, int slot) {
		this.name = name;
		this.slot = slot;
	}

	@Override
	public void evaluate(Scope<JsonNode> scope, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		EvaluationFrame<JsonNode> frame = scope.getEvaluationFrame();
		JsonNode value = frame != null ? frame.getValue(slot) : null;
		consumer.accept(name, JsonNodeUtils.nullToNullNode(scope.jsonProvider(), value));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
