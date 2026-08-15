package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Scope.ValueWithPath;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalVariableAccess implements Expression {
	private final String name;

	public ResolvedLocalVariableAccess(String name) {
		this.name = name;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		ValueWithPath<JsonNode> value = scope.getValueWithPath(name);
		if (value != null) {
			output.emit(value.value(), null);
			return;
		}
		throw new JsonQueryException(String.format("Local variable $%s is not set", name));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
