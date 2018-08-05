package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class VariableAccess implements Expression {
	private String name;

	public VariableAccess(final String name) {
		this.name = name;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final JsonNode value = scope.getValue(name);
		if (value == null)
			throw new JsonQueryException("Undefined variable: $" + name);
		output.emit(value);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
