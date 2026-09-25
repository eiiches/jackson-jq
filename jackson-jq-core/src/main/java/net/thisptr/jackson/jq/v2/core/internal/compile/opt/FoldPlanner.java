package net.thisptr.jackson.jq.v2.core.internal.compile.opt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.ConstantFoldingOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;

/**
 * Records fold metadata during lowering and immutably rewrites expressions from the top down afterwards.
 *
 * <p>Nothing is folded while lowering. Type checking reads the tree the query was written as, so every
 * fold happens in the single rewrite the root region runs once type checking is done. Function arguments
 * still open a region of their own, but only for the barrier bookkeeping {@link #closeRegion} describes;
 * the rewrite reaches them through their enclosing call, which rebinds itself so that a {@code Function}
 * specializing on {@link ConstantExpression} still sees an argument that folded.</p>
 */
public final class FoldPlanner {
	private record Metadata(boolean barrierFree, int frameSize, int globalCount, int outputCounterCount) {
		Metadata merge(Metadata other) {
			return new Metadata(
					barrierFree && other.barrierFree,
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

	private final Map<AnalyzedExpression<?>, Metadata> metadataByExpression = new IdentityHashMap<>();
	private final Map<AnalyzedExpression<?>, AnalyzedExpression<?>> optimizedByExpression = new IdentityHashMap<>();

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

	/**
	 * Closes the innermost region without rewriting it, undoing the barriers it installed.
	 * <p>
	 * A region's own barriers are undone so that one inside a function argument does not make the enclosing
	 * call unfoldable: the argument is a complete expression, folding the call re-evaluates it in place, and
	 * nothing outside it can observe what its barrier installed. This is also the unwinding path when
	 * lowering a region throws.
	 */
	public void closeRegion() {
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
	public <N> AnalyzedExpression<N> endNode(int mark, AnalyzedExpression<N> expression, int frameSize, int globalCount, int outputCounterCount) {
		Metadata metadata = new Metadata(mark == barriers, frameSize, globalCount, outputCounterCount);
		metadataByExpression.merge(expression, metadata, Metadata::merge);
		return expression;
	}

	/**
	 * Closes the innermost region and rewrites it, folding maximal constant subtrees.
	 * <p>
	 * Only the root region is finished this way, and only once type checking has run, so no fold can change
	 * a type the checker inferred or hide a diagnostic it would have reported.
	 *
	 * @param env the environment the query is compiling against
	 * @param root the expression the region lowered to
	 * @param <N> the JSON node type
	 * @return the rewritten expression
	 */
	public <N> AnalyzedExpression<N> finishRegion(Environment<N> env, AnalyzedExpression<N> root) {
		AnalyzedExpression<N> optimized = optimizeExpression(env, root);
		closeRegion();
		return optimized;
	}

	public <N> AnalyzedExpression<N> optimizeExpression(Environment<N> env, AnalyzedExpression<N> expression) {
		AnalyzedExpression<?> cached = optimizedByExpression.get(expression);
		if (cached != null) {
			@SuppressWarnings("unchecked")
			AnalyzedExpression<N> typed = (AnalyzedExpression<N>) cached;
			return typed;
		}

		Metadata metadata = metadataByExpression.get(expression);
		if (metadata != null && metadata.barrierFree()
				&& !(expression instanceof ConstantExpression<?, ?>)
				&& !expression.dependsOnInput()
				&& !expression.dependsOnExternalState()
				&& !FreeVariables.dependsOnVariables(expression)) {
			AnalyzedExpression<N> folded = folder.fold(env, expression, metadata.frameSize(), metadata.globalCount(), metadata.outputCounterCount());
			if (folded != expression) {
				optimizedByExpression.put(expression, folded);
				return folded;
			}
		}

		AnalyzedExpression<N> optimized = rewriteChildren(env, expression);
		optimizedByExpression.put(expression, optimized);
		return optimized;
	}

	public void transferMetadata(AnalyzedExpression<?> source, AnalyzedExpression<?> replacement) {
		if (source == replacement)
			return;
		Metadata metadata = metadataByExpression.get(source);
		if (metadata != null)
			metadataByExpression.merge(replacement, metadata, Metadata::merge);
	}

	private <N> AnalyzedExpression<N> rewriteChildren(Environment<N> env, AnalyzedExpression<N> expression) {
		if (!(expression instanceof RewritableExpression<?> rewritableExpr))
			return expression;
		// The instanceof check guarantees that this expression's JSON node type matches the current tree.
		@SuppressWarnings("unchecked")
		RewritableExpression<N> rewritable = (RewritableExpression<N>) rewritableExpr;
		return rewritable.rewriteChildren(child -> optimizeExpression(env, child));
	}
}
