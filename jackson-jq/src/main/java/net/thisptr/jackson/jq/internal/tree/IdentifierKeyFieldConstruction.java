package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	public final String key;
	public final Expression<JsonNode> value;

	public IdentifierKeyFieldConstruction(final String key, final Expression<JsonNode> value) {
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(final String key) {
		this(key, null);
	}

	@Override
	public void evaluate(final Scope<JsonNode> scope, final JsonNode in, final FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		if (value == null) {
			consumer.accept(key, JsonNodeUtils.nullToNullNode(scope.jsonProvider(), scope.jsonProvider().get(in, key)));
		} else {
			value.apply(scope, in, (v) -> consumer.accept(key, v));
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
