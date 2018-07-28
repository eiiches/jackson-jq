package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class StringKeyFieldConstruction implements FieldConstruction {
	private final JsonQuery key;
	private final JsonQuery value;

	public StringKeyFieldConstruction(final JsonQuery key, final JsonQuery value) {
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstruction(final JsonQuery key) {
		this(key, null);
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		for (final JsonNode k : key.apply(scope, in)) {
			if (!k.isTextual())
				throw new JsonQueryException("key must evaluate to string");
			if (value == null) {
				consumer.accept(k.asText(), JsonNodeUtils.nullToNullNode(in.get(k.asText())));
			} else {
				for (final JsonNode v : value.apply(scope, in))
					consumer.accept(k.asText(), v);
			}
		}
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
