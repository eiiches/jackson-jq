package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionDefinition implements Expression {
	private final int slot;
	private final FunctionFactory factory;

	public ResolvedFunctionDefinition(int slot, FunctionFactory factory) {
		this.slot = slot;
		this.factory = factory;
	}

	public int slot() {
		return slot;
	}

	public FunctionFactory factory() {
		return factory;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		scope.setFunctionFactory(slot, factory);
	}
}
