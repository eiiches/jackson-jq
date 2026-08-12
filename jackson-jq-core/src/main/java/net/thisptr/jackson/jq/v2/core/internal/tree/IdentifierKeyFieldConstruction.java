package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	public final String key;
	public final @Nullable Expression<JsonNode> value;

	public IdentifierKeyFieldConstruction(String key, @Nullable Expression<JsonNode> value) {
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(String key) {
		this(key, null);
	}

	@Override
	public void evaluate(Scope<JsonNode> scope, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
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
