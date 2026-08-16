package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class StringKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	public final Expression key;
	public final @Nullable Expression value;

	public StringKeyFieldConstruction(Expression key, @Nullable Expression value) {
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstruction(Expression key) {
		this(key, null);
	}

	@Override
	public void evaluate(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(jsonProvider, frame, in, (k) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryException("key must evaluate to string");
			if (value == null) {
				consumer.accept(jsonProvider.asText(k), JsonNodeUtils.nullToNullNode(jsonProvider, jsonProvider.get(in, jsonProvider.asText(k))));
			} else {
				value.apply(jsonProvider, frame, in, (v) -> consumer.accept(jsonProvider.asText(k), v));
			}
		});
	}

	@Override
	public String toString() {
		if (value == null) {
			return key.toString();
		} else {
			return key.toString() + ": " + value.toString();
		}
	}
}
