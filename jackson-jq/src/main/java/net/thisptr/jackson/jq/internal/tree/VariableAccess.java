package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Scope.ValueWithPath;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class VariableAccess implements Expression {
	private String name;

	public VariableAccess(final String name) {
		this.name = name;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final ValueWithPath value = scope.getValueWithPath(name);
		if (value == null)
			throw new JsonQueryException("$" + name + " is not defined");
		output.emit(value.value(), value.path());
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
