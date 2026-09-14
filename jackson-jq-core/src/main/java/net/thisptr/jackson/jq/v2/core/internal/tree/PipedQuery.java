package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class PipedQuery<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> left;
	private final Expression<StackFrame, JsonNode> right;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public PipedQuery(Expression<StackFrame, JsonNode> left, Expression<StackFrame, JsonNode> right) {
		this.left = left;
		this.right = right;
		this.dependsOnInput = left.dependsOnInput() || right.dependsOnInput();
		this.dependsOnExternalState = left.dependsOnExternalState() || right.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.union(left, right);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(left, right);
	}

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(left.getCardinality(), right.getCardinality());
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
		left.apply(frame, in, path, (value, outputPath) -> {
			Path<JsonNode> nextPath = !(path instanceof UntrackedPath) && outputPath instanceof UntrackedPath ? UnrepresentablePath.getInstance() : outputPath;
			right.apply(frame, value, nextPath, output);
		});
	}
}
