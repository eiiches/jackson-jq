package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<JsonNode> key;
	private final Expression<JsonNode> value;

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> key, Expression<JsonNode> value) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
	}

	public Expression<JsonNode> key() {
		return key;
	}

	public Expression<JsonNode> value() {
		return value;
	}

	@Override
	public void evaluate(@Nullable StackFrame<JsonNode> frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(frame, in, (k) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot use %s as object key", k);
			value.apply(frame, in, (v) -> consumer.accept(jsonProvider.asText(k), v));
		});
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
