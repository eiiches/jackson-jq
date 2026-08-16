package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final Expression key;
	private final Expression value;

	public JsonQueryKeyFieldConstruction(Expression key, Expression value) {
		this.key = key;
		this.value = value;
	}

	public Expression key() {
		return key;
	}

	public Expression value() {
		return value;
	}

	@Override
	public void evaluate(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(jsonProvider, frame, in, (k) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot use %s as object key", k);
			value.apply(jsonProvider, frame, in, (v) -> consumer.accept(jsonProvider.asText(k), v));
		});
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
