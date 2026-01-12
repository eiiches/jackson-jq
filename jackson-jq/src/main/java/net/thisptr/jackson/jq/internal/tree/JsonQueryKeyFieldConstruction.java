package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final Expression<JsonNode> key;
	private final Expression<JsonNode> value;

	public JsonQueryKeyFieldConstruction(final Expression<JsonNode> key, final Expression<JsonNode> value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public void evaluate(final Scope<JsonNode> scope, final JsonNode in, final FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(scope, in, (k) -> {
			if (scope.jsonProvider().getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(scope.jsonProvider(), "Cannot use %s as object key", k);
			value.apply(scope, in, (v) -> consumer.accept(scope.jsonProvider().asText(k), v));
		});
	}

	@Override
	public String toString() {
		final String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
