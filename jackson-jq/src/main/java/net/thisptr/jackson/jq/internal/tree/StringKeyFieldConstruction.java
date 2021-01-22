package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class StringKeyFieldConstruction implements FieldConstruction {
	private final Expression key;
	private final Expression value;

	public StringKeyFieldConstruction(final Expression key, final Expression value) {
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstruction(final Expression key) {
		this(key, null);
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		key.apply(scope, in, (k) -> {
			if (!k.isTextual())
				throw new JsonQueryException("key must evaluate to string");
			if (value == null) {
				consumer.accept(k.asText(), JsonNodeUtils.nullToNullNode(in.get(k.asText())));
			} else {
				value.apply(scope, in, (v) -> consumer.accept(k.asText(), v));
			}
		});
	}

	@Override
	public String toString() {
		if (value == null) {
			return key.toString();
		} else {
			return key + ": " + value;
		}
	}
}
