package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
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
	public void evaluate(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		JsonNode value = frame != null ? frame.getValueNode(0) : null;
		consumer.accept(name, JsonNodeUtils.nullToNullNode(jsonProvider, value));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
