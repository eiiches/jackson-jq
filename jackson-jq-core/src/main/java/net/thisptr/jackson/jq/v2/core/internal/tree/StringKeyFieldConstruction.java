package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class StringKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final Expression<JsonNode> key;
	public final @Nullable Expression<JsonNode> value;

	public StringKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> key, @Nullable Expression<JsonNode> value) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> key) {
		this(jsonProvider, key, null);
	}

	@Override
	public void evaluate(@Nullable StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(frame, in, (k) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryException("key must evaluate to string");
			if (value == null) {
				consumer.accept(jsonProvider.asText(k), JsonNodeUtils.nullToNullNode(jsonProvider, jsonProvider.get(in, jsonProvider.asText(k))));
			} else {
				value.apply(frame, in, (v) -> consumer.accept(jsonProvider.asText(k), v));
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
