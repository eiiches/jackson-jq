package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionCall<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> function;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ResolvedFunctionCall(Expression<StackFrame, JsonNode> function, boolean dependsOnExternalState, boolean dependsOnInput, boolean inputFixed, List<Expression<StackFrame, JsonNode>> args) {
		this.function = function;
		this.dependsOnInput = (dependsOnInput && !inputFixed) || args.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = dependsOnExternalState || args.stream().anyMatch(Expression::dependsOnExternalState);
		this.freeLocalSlots = FreeVariables.unionAll(args);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(args);
	}

	public Expression<StackFrame, JsonNode> function() {
		return function;
	}

	@Override
	public Cardinality getCardinality() {
		return function.getCardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		function.apply(frame, in, path, output);
	}
}
