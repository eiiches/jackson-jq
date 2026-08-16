package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	public final String key;
	public final @Nullable Expression value;

	public IdentifierKeyFieldConstruction(String key, @Nullable Expression value) {
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(String key) {
		this(key, null);
	}

	@Override
	public void evaluate(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		if (value == null) {
			consumer.accept(key, JsonNodeUtils.nullToNullNode(jsonProvider, jsonProvider.get(in, key)));
		} else {
			value.apply(jsonProvider, frame, in, (v) -> consumer.accept(key, v));
		}
	}

	@Override
	public String toString() {
		if (value == null) {
			return key;
		} else {
			return key + ": " + value.toString();
		}
	}
}
