package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class VariableKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final String name;

	public VariableKeyFieldConstruction(final String name) {
		this.name = name;
	}

	@Override
	public void evaluate(final Scope<JsonNode> scope, final JsonNode in, final FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		consumer.accept(name, JsonNodeUtils.nullToNullNode(scope.jsonProvider(), scope.getValue(name)));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
