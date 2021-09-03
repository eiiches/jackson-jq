package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class VariableKeyFieldConstruction implements FieldConstruction {
	private String name;

	public VariableKeyFieldConstruction(final String name) {
		this.name = name;
	}

	public VariableKeyFieldConstruction() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void evaluate(final Scope scope, final JsonNode in, final FieldConsumer consumer) throws JsonQueryException {
		consumer.accept(name, JsonNodeUtils.nullToNullNode(scope.getValue(name)));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
