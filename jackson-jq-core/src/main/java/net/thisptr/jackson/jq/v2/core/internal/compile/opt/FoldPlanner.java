package net.thisptr.jackson.jq.v2.core.internal.compile.opt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.ConstantFoldingOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * Records fold metadata during lowering and immutably rewrites expressions from the top down afterwards.
 *
 * <p>Function arguments form their own regions because they must be finalized before
 * {@code Function.bind} can specialize from {@link ConstantExpression}.</p>
 */
public final class FoldPlanner {
	private static final class Metadata {
		final boolean foldable;
		final int frameSize;
		final int globalCount;
		final int outputCounterCount;

		Metadata(boolean foldable, int frameSize, int globalCount, int outputCounterCount) {
			this.foldable = foldable;
			this.frameSize = frameSize;
			this.globalCount = globalCount;
			this.outputCounterCount = outputCounterCount;
		}

		Metadata merge(Metadata other) {
			return new Metadata(foldable && other.foldable,
					Math.max(frameSize, other.frameSize),
					Math.max(globalCount, other.globalCount),
					Math.max(outputCounterCount, other.outputCounterCount));
		}
	}

	private final ConstantFolder folder;

	// The barrier count each open region started at. A region's own barriers are undone when it closes, so
	// that a barrier inside a function argument does not make the enclosing call unfoldable: the argument is
	// a complete expression, folding the call re-evaluates it in place, and nothing outside it can observe
	// what its barrier installed.
	private final Deque<Integer> regionBarriers = new ArrayDeque<>();

	// Bumped by every barrier. A node contains one exactly when this moved while it was being compiled, so
	// propagation to enclosing nodes needs no per-node bookkeeping -- see beginNode/endNode.
	private int barriers;

	private final Map<Expression<StackFrame, ?>, Metadata> metadataByExpression = new IdentityHashMap<>();
	private final Map<Expression<StackFrame, ?>, Expression<StackFrame, ?>> optimizedByExpression = new IdentityHashMap<>();

	public FoldPlanner(ConstantFoldingOptions options) {
		this.folder = new ConstantFolder(options);
	}

	public boolean isEnabled() {
		return folder.isEnabled();
	}

	public boolean isPlanning() {
		return !regionBarriers.isEmpty();
	}

	public void beginRegion() {
		regionBarriers.push(barriers);
	}

	public void cancelRegion() {
		barriers = regionBarriers.pop();
	}

	/**
	 * Marks a node whose presence makes every subtree containing it unfoldable. See
	 * {@code CompileContext#markFoldBarrier()} for what qualifies.
	 */
	public void markBarrier() {
		++barriers;
	}

	/**
	 * Opens a node. The returned mark is the only state a node needs; an abandoned node is simply one whose
	 * {@link #endNode} is never reached.
	 *
	 * @return the mark to hand back to {@link #endNode}
	 */
	public int beginNode() {
		return barriers;
	}

	/**
	 * Records what {@link #finishRegion} needs to know about a freshly lowered node.
	 *
	 * @param mark the value {@link #beginNode} returned for this node
	 * @param expression the lowered expression
	 * @param frameSize the frame size required by this expression's compile position
	 * @param globalCount the number of global slots assigned so far
	 * @param outputCounterCount the number of output counters assigned so far
	 * @param <N> the JSON node type
	 * @return {@code expression}, unchanged
	 */
	public <N> Expression<StackFrame, N> endNode(int mark, Expression<StackFrame, N> expression, int frameSize, int globalCount, int outputCounterCount) {
		boolean foldable = mark == barriers
				&& !(expression instanceof ConstantExpression<?, ?>)
				&& !expression.dependsOnInput()
				&& !expression.dependsOnExternalState()
				&& !FreeVariables.dependsOnVariables(expression);
		Metadata metadata = new Metadata(foldable, frameSize, globalCount, outputCounterCount);
		metadataByExpression.merge(expression, metadata, Metadata::merge);
		return expression;
	}

	/**
	 * Closes the innermost region and rewrites it, folding maximal constant subtrees.
	 *
	 * @param env the environment the query is compiling against
	 * @param root the expression the region lowered to
	 * @param <N> the JSON node type
	 * @return the rewritten expression
	 */
	public <N> Expression<StackFrame, N> finishRegion(Environment<N> env, Expression<StackFrame, N> root) {
		Expression<StackFrame, N> optimized = optimize(env, root);
		barriers = regionBarriers.pop();
		return optimized;
	}

	private <N> Expression<StackFrame, N> optimize(Environment<N> env, Expression<StackFrame, N> expression) {
		Expression<StackFrame, ?> cached = optimizedByExpression.get(expression);
		if (cached != null) {
			@SuppressWarnings("unchecked")
			Expression<StackFrame, N> typed = (Expression<StackFrame, N>) cached;
			return typed;
		}

		Metadata metadata = metadataByExpression.get(expression);
		if (metadata != null && metadata.foldable) {
			Expression<StackFrame, N> folded = folder.fold(env, expression, metadata.frameSize, metadata.globalCount, metadata.outputCounterCount);
			if (folded != expression) {
				optimizedByExpression.put(expression, folded);
				return folded;
			}
		}

		Expression<StackFrame, N> optimized = rewriteChildren(env, expression);
		optimizedByExpression.put(expression, optimized);
		return optimized;
	}

	private <N> Expression<StackFrame, N> rewriteChildren(Environment<N> env, Expression<StackFrame, N> expression) {
		if (!(expression instanceof RewritableExpression<?>))
			return expression;
		// The instanceof check guarantees that this expression's JSON node type matches the current tree.
		@SuppressWarnings("unchecked")
		RewritableExpression<N> rewritable = (RewritableExpression<N>) expression;
		return rewritable.rewriteChildren(child -> optimize(env, child));
	}
}
