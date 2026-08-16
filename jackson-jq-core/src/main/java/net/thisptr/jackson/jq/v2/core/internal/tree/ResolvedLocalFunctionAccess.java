package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalFunctionAccess implements Expression {
	private final String name;
	private final int slot;
	private final List<Expression> args;

	public ResolvedLocalFunctionAccess(String name, int slot, List<Expression> args) {
		this.name = name;
		this.slot = slot;
		this.args = args;
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	public List<Expression> args() {
		return args;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		FunctionFactory factory = scope.getFunctionFactory(slot);
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		Function<JsonNode> fn = factory.createFunction(scope.jsonProvider(), args, Versions.JQ_1_7);
		fn.apply(scope, in, ipath, output);
	}

	@Override
	public String toString() {
		return name;
	}
}
