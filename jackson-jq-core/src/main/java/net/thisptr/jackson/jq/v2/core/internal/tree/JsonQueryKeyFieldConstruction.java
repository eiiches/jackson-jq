package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<JsonNode> key;
	private final Expression<JsonNode> value;
	private final @Nullable Version version;

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> key, Expression<JsonNode> value) {
		this(jsonProvider, key, value, null);
	}

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> key, Expression<JsonNode> value, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
	}

	public Expression<JsonNode> key() {
		return key;
	}

	public Expression<JsonNode> value() {
		return value;
	}

	@Override
	public void evaluate(@Nullable StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(frame, in, null, (k, opath) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot use %s as object key", k);
			value.apply(frame, in, null, (v, opath2) -> consumer.accept(jsonProvider.asText(k), v));
		});
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
