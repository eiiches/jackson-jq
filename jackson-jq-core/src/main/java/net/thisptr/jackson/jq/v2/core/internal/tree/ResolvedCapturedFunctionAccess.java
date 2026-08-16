package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedCapturedFunctionAccess implements Expression {
	private final String name;
	private final int closureSlot;
	private final List<Expression> args;

	public ResolvedCapturedFunctionAccess(String name, int closureSlot, List<Expression> args) {
		this.name = name;
		this.closureSlot = closureSlot;
		this.args = args;
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
	}

	public List<Expression> args() {
		return args;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		ExecutionStack<JsonNode>.Frame frame = scope.getExecutionFrame();
		Closure<JsonNode> closure = frame != null ? frame.getClosure() : null;
		FunctionFactory factory = closure != null ? closure.getFunctionFactory(closureSlot) : null;
		if (factory == null) {
			throw new JsonQueryException("Function " + name + " is not defined");
		}
		Function<JsonNode> fn = factory.createFunction(scope.jsonProvider(), args, Versions.JQ_1_7);
		fn.apply(scope, in, path, output);
	}

	@Override
	public String toString() {
		return name;
	}
}
