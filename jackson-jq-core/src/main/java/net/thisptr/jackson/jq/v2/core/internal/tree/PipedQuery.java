package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class PipedQuery<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final AnalyzedExpression<JsonNode> left;
	private final AnalyzedExpression<JsonNode> right;
	// Counter for everything `left` emits. `right` needs none: its values are piped straight out as this
	// query's own, so whatever consumes this query charges them.
	private final int leftOutputIndex;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public PipedQuery(AnalyzedExpression<JsonNode> left, AnalyzedExpression<JsonNode> right, int leftOutputIndex) {
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
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> newLeft = rewriter.rewrite(left);
		AnalyzedExpression<JsonNode> newRight = rewriter.rewrite(right);
		return newLeft == left && newRight == right ? this : new PipedQuery<>(newLeft, newRight, leftOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		if (path instanceof UntrackedPath) {
			// No path is being tracked, and neither stage can start one, so there is nothing to record.
			left.apply(frame, in, path, (value, outputPath) -> {
				memory.countOutput(leftOutputIndex);
				right.apply(frame, value, outputPath, output);
			});
			return;
		}
		// A pipe is where a path goes stale: whichever stage fails to produce one, the position the
		// traversal had reached is the path and value that stage was handed. jq keeps that pair so a
		// later step, or path/1, can resume from it.
		left.apply(frame, in, path, (value, outputPath) -> {
			memory.countOutput(leftOutputIndex);
			Path<JsonNode> nextPath = PathUtils.stale(outputPath, path, in);
			right.apply(frame, value, nextPath, (rightValue, rightPath) -> {
				output.emit(rightValue, PathUtils.stale(rightPath, nextPath, value));
			});
		});
	}
}
