package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionCall implements Expression {
	private final String name;
	private final Function function;

	public ResolvedFunctionCall(String name, Function function) {
		this.name = name;
		this.function = function;
	}

	public Function function() {
		return function;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		function.apply(in, path, output);
	}

	@Override
	public String toString() {
		return name + "()";
	}
}
