package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ForeachExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> iterExpr;
	private final Expression<StackFrame, JsonNode> updateExpr;
	private final Expression<StackFrame, JsonNode> initExpr;
	private final @Nullable Expression<StackFrame, JsonNode> extractExpr;
	private final PatternMatcher<JsonNode> matcher;

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

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression<StackFrame, JsonNode> initExpr, Expression<StackFrame, JsonNode> updateExpr, @Nullable Expression<StackFrame, JsonNode> extractExpr, Expression<StackFrame, JsonNode> iterExpr, Set<Integer> matcherSlots) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
		// updateExpr/extractExpr are already compiled under the correct shielded context (see
		// Compiler's ForeachExpressionAstNode handling), so dependsOnInput/dependsOnExternalState are
		// a flat OR, same as everywhere else. The matcher's bound slot(s) are only "closed" for
		// updateExpr/extractExpr -- they're not yet bound while initExpr/iterExpr run.
		this.dependsOnInput = initExpr.dependsOnInput() || iterExpr.dependsOnInput() || updateExpr.dependsOnInput()
				|| (extractExpr != null && extractExpr.dependsOnInput());
		this.dependsOnExternalState = initExpr.dependsOnExternalState() || iterExpr.dependsOnExternalState() || updateExpr.dependsOnExternalState()
				|| (extractExpr != null && extractExpr.dependsOnExternalState());
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(initExpr, iterExpr, updateExpr, extractExpr);
		this.freeLocalSlots = FreeVariables.minus(
				FreeVariables.union(initExpr, iterExpr, updateExpr, extractExpr),
				new ArrayList<>(matcherSlots));
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		initExpr.apply(frame, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			@SuppressWarnings("unchecked")
			Path<JsonNode>[] accumulatorPaths = (Path<JsonNode>[]) new Path<?>[] { accumulatorPath };

			iterExpr.apply(frame, in, ipath, (item, itemPath) -> {
				Deque<PatternMatcher.MatchWithPath<JsonNode>> stack = new ArrayDeque<>();
				matcher.matchWithPath(frame, item, itemPath, (Deque<PatternMatcher.MatchWithPath<JsonNode>> vars) -> {
					for (Iterator<PatternMatcher.MatchWithPath<JsonNode>> it = vars.descendingIterator(); it.hasNext(); ) {
						PatternMatcher.MatchWithPath<JsonNode> var = it.next();
						if (var.slot >= 0) {
							frame.set(var.slot, var.path instanceof UntrackedPath ? var.value : new PathAndValue<>(var.path, var.value));
						}
					}

					updateExpr.apply(frame, accumulators[0], extractExpr != null ? UntrackedPath.getInstance() : accumulatorPaths[0], (newaccumulator, newaccumulatorPath) -> {
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
				}, stack);
			});
		});
	}
}
