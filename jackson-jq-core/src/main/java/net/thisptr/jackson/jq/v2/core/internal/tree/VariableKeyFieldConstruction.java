package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class VariableKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final String name;

	public VariableKeyFieldConstruction(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public void evaluate(Scope<JsonNode> scope, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		net.thisptr.jackson.jq.v2.spi.ExecutionStack<JsonNode>.Frame frame = scope.getExecutionFrame();
		JsonNode value = frame != null ? frame.getValueNode(0) : null;
		consumer.accept(name, JsonNodeUtils.nullToNullNode(scope.jsonProvider(), value));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
