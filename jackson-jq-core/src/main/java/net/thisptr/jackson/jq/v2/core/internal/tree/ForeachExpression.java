package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ForeachExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final AnalyzedExpression<JsonNode> iterExpr;
	private final AnalyzedExpression<JsonNode> updateExpr;
	private final AnalyzedExpression<JsonNode> initExpr;
	private final @Nullable AnalyzedExpression<JsonNode> extractExpr;
	private final PatternMatcher<JsonNode> matcher;
	// `extractExpr` needs no counter: when present it emits this foreach's own values.
	private final int initOutputIndex;
	private final int updateOutputIndex;
	private final int iterOutputIndex;
	private final Set<Integer> matcherSlots;

	@Override
	public Cardinality getCardinality() {
		return extractExpr != null
				? CardinalityUtils.multiply(initExpr.getCardinality(), iterExpr.getCardinality(), updateExpr.getCardinality(), extractExpr.getCardinality())
				: CardinalityUtils.multiply(initExpr.getCardinality(), iterExpr.getCardinality(), updateExpr.getCardinality());
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ForeachExpression(PatternMatcher<JsonNode> matcher, AnalyzedExpression<JsonNode> initExpr, AnalyzedExpression<JsonNode> updateExpr, @Nullable AnalyzedExpression<JsonNode> extractExpr, AnalyzedExpression<JsonNode> iterExpr, Set<Integer> matcherSlots, int initOutputIndex, int updateOutputIndex, int iterOutputIndex) {
		this.matcher = matcher;
		this.initOutputIndex = initOutputIndex;
		this.updateOutputIndex = updateOutputIndex;
		this.iterOutputIndex = iterOutputIndex;
		this.matcherSlots = matcherSlots;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
		// updateExpr sees the accumulator rather than `.`, and extractExpr sees updateExpr's own output, so
		// both have their input dependency discharged by initExpr and iterExpr -- which between them
		// determine the accumulator. dependsOnExternalState stays a flat OR: rebinding `.` cannot make a
		// clock or a file read deterministic. The matcher's bound slot(s) are only "closed" for
		// updateExpr/extractExpr -- they're not yet bound while initExpr/iterExpr run.
		this.dependsOnInput = initExpr.dependsOnInput() || iterExpr.dependsOnInput();
		this.dependsOnExternalState = initExpr.dependsOnExternalState() || iterExpr.dependsOnExternalState() || updateExpr.dependsOnExternalState()
				|| (extractExpr != null && extractExpr.dependsOnExternalState());
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(initExpr, iterExpr, updateExpr, extractExpr);
		this.freeLocalSlots = FreeVariables.minus(
				FreeVariables.union(initExpr, iterExpr, updateExpr, extractExpr),
				new ArrayList<>(matcherSlots));
	}

	public AnalyzedExpression<JsonNode> iterExpr() {
		return iterExpr;
	}

	public AnalyzedExpression<JsonNode> initExpr() {
		return initExpr;
	}

	public AnalyzedExpression<JsonNode> updateExpr() {
		return updateExpr;
	}

	public @Nullable AnalyzedExpression<JsonNode> extractExpr() {
		return extractExpr;
	}

	public PatternMatcher<JsonNode> matcher() {
		return matcher;
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
		AnalyzedExpression<JsonNode> rewrittenIter = rewriter.rewrite(iterExpr);
		AnalyzedExpression<JsonNode> rewrittenInit = rewriter.rewrite(initExpr);
		PatternMatcher<JsonNode> rewrittenMatcher = matcher.rewriteExpressions(rewriter::rewrite);
		AnalyzedExpression<JsonNode> rewrittenUpdate = rewriter.rewrite(updateExpr);
		AnalyzedExpression<JsonNode> rewrittenExtract = extractExpr != null ? rewriter.rewrite(extractExpr) : null;
		return rewrittenIter == iterExpr && rewrittenInit == initExpr && rewrittenMatcher == matcher
				&& rewrittenUpdate == updateExpr && rewrittenExtract == extractExpr
				? this
				: new ForeachExpression<>(rewrittenMatcher, rewrittenInit, rewrittenUpdate, rewrittenExtract, rewrittenIter, matcherSlots, initOutputIndex, updateOutputIndex, iterOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		initExpr.apply(frame, in, ipath, (accumulator, accumulatorPath) -> {
			memory.countOutput(initOutputIndex);
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			@SuppressWarnings("unchecked")
			Path<JsonNode>[] accumulatorPaths = (Path<JsonNode>[]) new Path<?>[] { accumulatorPath };

			// The matcher binds its variables straight into the frame, so by the time onMatch runs
			// updateExpr can simply read them.
			PatternMatcher.OnMatch onMatch = () -> {
				updateExpr.apply(frame, accumulators[0], extractExpr != null ? UntrackedPath.getInstance() : accumulatorPaths[0], (newaccumulator, newaccumulatorPath) -> {
					memory.countOutput(updateOutputIndex);
					if (extractExpr != null) {
						extractExpr.apply(frame, newaccumulator, !(ipath instanceof UntrackedPath) && newaccumulatorPath instanceof UntrackedPath ? UnrepresentablePath.getInstance() : newaccumulatorPath, output);
					} else {
						output.emit(newaccumulator, newaccumulatorPath);
					}
					accumulators[0] = newaccumulator;
					accumulatorPaths[0] = !(ipath instanceof UntrackedPath) && newaccumulatorPath instanceof UntrackedPath
							? UnrepresentablePath.getInstance()
							: newaccumulatorPath;
				});
			};
			iterExpr.apply(frame, in, ipath, (item, itemPath) -> {
				memory.countOutput(iterOutputIndex);
				matcher.matchWithPath(frame, item, itemPath, onMatch);
			});
		});
	}
}
