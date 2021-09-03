package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class ValueLiteral implements Expression {
	private JsonNode value;

	public ValueLiteral() {}

	public ValueLiteral(final JsonNode value) {
		this.value = value;
	}

	public JsonNode getValue() {
		return value;
	}

	public void setValue(JsonNode value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		output.emit(value, null);
	}

	@Override
	public String toString() {
		return value.toString();
	}

	public JsonNode value() {
		return value;
	}
}
