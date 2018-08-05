package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public abstract class ValueLiteral implements Expression {
	private JsonNode value;

	public ValueLiteral(final JsonNode value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		output.emit(value);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
