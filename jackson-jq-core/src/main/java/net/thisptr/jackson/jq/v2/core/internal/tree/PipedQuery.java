package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class PipedQuery<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> left;
	private final Expression<StackFrame, JsonNode> right;
	// Counter for everything `left` emits. `right` needs none: its values are piped straight out as this
	// query's own, so whatever consumes this query charges them.
	private final int leftOutputIndex;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public PipedQuery(Expression<StackFrame, JsonNode> left, Expression<StackFrame, JsonNode> right, int leftOutputIndex) {
		this.left = left;
		this.right = right;
		this.leftOutputIndex = leftOutputIndex;
		// Not an OR: the right side's `.` is whatever the left side emitted, so a pipe needs its caller's
		// input exactly when its left side does. `1 | . + 1` therefore depends on no input at all.
		this.dependsOnInput = left.dependsOnInput();
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
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		Expression<StackFrame, JsonNode> newLeft = rewriter.rewrite(left);
		Expression<StackFrame, JsonNode> newRight = rewriter.rewrite(right);
		return newLeft == left && newRight == right ? this : new PipedQuery<>(newLeft, newRight, leftOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		left.apply(frame, in, path, (value, outputPath) -> {
			memory.countOutput(leftOutputIndex);
			Path<JsonNode> nextPath = !(path instanceof UntrackedPath) && outputPath instanceof UntrackedPath ? UnrepresentablePath.getInstance() : outputPath;
			right.apply(frame, value, nextPath, output);
		});
	}
}
