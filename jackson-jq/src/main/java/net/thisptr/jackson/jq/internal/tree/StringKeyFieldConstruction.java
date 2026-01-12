package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class StringKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	public final Expression<JsonNode> key;
	public final Expression<JsonNode> value;

	public StringKeyFieldConstruction(final Expression<JsonNode> key, final Expression<JsonNode> value) {
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstruction(final Expression<JsonNode> key) {
		this(key, null);
	}

	@Override
	public void evaluate(final Scope<JsonNode> scope, final JsonNode in, final FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(scope, in, (k) -> {
			if (scope.jsonProvider().getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryException("key must evaluate to string");
			if (value == null) {
				consumer.accept(scope.jsonProvider().asText(k), JsonNodeUtils.nullToNullNode(scope.jsonProvider(), scope.jsonProvider().get(in, scope.jsonProvider().asText(k))));
			} else {
				value.apply(scope, in, (v) -> consumer.accept(scope.jsonProvider().asText(k), v));
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
