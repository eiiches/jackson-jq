package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public class JsonQueryKeyFieldConstruction implements FieldConstruction {
	private Expression key;
	private Expression value;

	public JsonQueryKeyFieldConstruction(final Expression key, final Expression value) {
		this.key = key;
		this.value = value;
	}

	public JsonQueryKeyFieldConstruction() {}

	public Expression getValue() {
		return value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	public Expression getKey() {
		return key;
	}

	public void setKey(Expression key) {
		this.key = key;
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		key.apply(scope, in, (k) -> {
			if (!k.isTextual())
				throw new JsonQueryTypeException("Cannot use %s as object key", k);
			value.apply(scope, in, (v) -> consumer.accept(k.asText(), v));
		});
	}

	@Override
	public String toString() {
		final String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
