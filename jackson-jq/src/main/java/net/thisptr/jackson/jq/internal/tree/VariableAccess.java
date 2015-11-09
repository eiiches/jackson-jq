package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class VariableAccess extends JsonQuery {
	private String name;

	public VariableAccess(final String name) {
		this.name = name;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final JsonNode value = scope.getValue(name);
		if (value == null)
			throw new JsonQueryException("Undefined variable: $" + name);
		return Collections.singletonList(value);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
