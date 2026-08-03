package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class IdentifierKeyFieldConstruction implements FieldConstruction {
	public final String key;
	public final Expression value;

	public IdentifierKeyFieldConstruction(final String key, final Expression value) {
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(final String key) {
		this(key, null);
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		if (value == null) {
			consumer.accept(key, JsonNodeUtils.nullToNullNode(in.get(key)));
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
