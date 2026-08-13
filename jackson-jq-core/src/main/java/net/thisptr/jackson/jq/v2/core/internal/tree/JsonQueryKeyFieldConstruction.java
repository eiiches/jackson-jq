package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final Expression key;
	private final Expression value;

	public JsonQueryKeyFieldConstruction(Expression key, Expression value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public void evaluate(Scope<JsonNode> scope, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(scope, in, (k) -> {
			if (scope.jsonProvider().getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(scope.jsonProvider(), "Cannot use %s as object key", k);
			value.apply(scope, in, (v) -> consumer.accept(scope.jsonProvider().asText(k), v));
		});
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
