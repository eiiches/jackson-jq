package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public class JsonQueryKeyFieldConstruction implements FieldConstruction {
	private final JsonQuery key;
	private final JsonQuery value;

	public JsonQueryKeyFieldConstruction(final JsonQuery key, final JsonQuery value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		for (final JsonNode k : key.apply(scope, in)) {
			if (!k.isTextual())
				throw new JsonQueryTypeException("Cannot use %s as object key", k);
			for (final JsonNode v : value.apply(scope, in))
				consumer.accept(k.asText(), v);
		}
	}

	@Override
	public String toString() {
		final String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
