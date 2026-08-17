package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final String key;
	public final @Nullable Expression<JsonNode> value;

	public IdentifierKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, String key, @Nullable Expression<JsonNode> value) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, String key) {
		this(jsonProvider, key, null);
	}

	@Override
	public void evaluate(@Nullable StackFrame<JsonNode> frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		if (value == null) {
			consumer.accept(key, JsonNodeUtils.nullToNullNode(jsonProvider, jsonProvider.get(in, key)));
		} else {
			value.apply(frame, in, (v) -> consumer.accept(key, v));
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
