package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class IdentifierKeyFieldConstruction implements FieldConstruction {
	private final String key;
	private final JsonQuery value;

	public IdentifierKeyFieldConstruction(final String key, final JsonQuery value) {
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
			for (final JsonNode v : value.apply(scope, in))
				consumer.accept(key, v);
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
