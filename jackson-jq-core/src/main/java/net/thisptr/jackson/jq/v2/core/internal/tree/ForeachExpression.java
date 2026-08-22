package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ForeachExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> updateExpr;
	private Expression<JsonNode> initExpr;
	private @Nullable Expression<JsonNode> extractExpr;
	private PatternMatcher<JsonNode> matcher;

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression<JsonNode> initExpr, Expression<JsonNode> updateExpr, @Nullable Expression<JsonNode> extractExpr, Expression<JsonNode> iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcher<JsonNode> matcher() { return matcher; }
	public Expression<JsonNode> initExpr() { return initExpr; }
	public Expression<JsonNode> updateExpr() { return updateExpr; }
	public @Nullable Expression<JsonNode> extractExpr() { return extractExpr; }
	public Expression<JsonNode> iterExpr() { return iterExpr; }

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		initExpr.apply(frame, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			@SuppressWarnings("unchecked")
			Path<JsonNode>[] accumulatorPaths = (Path<JsonNode>[]) new Path<?>[] { accumulatorPath };

			iterExpr.apply(frame, in, ipath, (item, itemPath) -> {
				Deque<PatternMatcher.MatchWithPath<JsonNode>> stack = new ArrayDeque<>();
				matcher.matchWithPath(frame, item, itemPath, (Deque<PatternMatcher.MatchWithPath<JsonNode>> vars) -> {
					for (Iterator<PatternMatcher.MatchWithPath<JsonNode>> it = vars.descendingIterator(); it.hasNext();) {
						PatternMatcher.MatchWithPath<JsonNode> var = it.next();
						if (frame != null && var.slot >= 0) {
							frame.set(var.slot, var.path != null ? new PathAndValue<>(var.path, var.value) : var.value);
						}
					}

					updateExpr.apply(frame, accumulators[0], extractExpr != null ? null : accumulatorPaths[0], (newaccumulator, newaccumulatorPath) -> {
						if (extractExpr != null) {
							extractExpr.apply(frame, newaccumulator, ipath != null && newaccumulatorPath == null ? UnrepresentablePath.getInstance() : newaccumulatorPath, output);
						} else {
							output.emit(newaccumulator, newaccumulatorPath);
						}
						accumulators[0] = newaccumulator;
						accumulatorPaths[0] = ipath != null && newaccumulatorPath == null
								? UnrepresentablePath.getInstance()
								: newaccumulatorPath;
					});
				}, stack);
			});
		});
	}

	@Override
	public String toString() {
		if (extractExpr == null) {
			return String.format("(foreach %s as %s (%s; %s))", iterExpr, matcher, initExpr, updateExpr);
		} else {
			return String.format("(foreach %s as %s (%s; %s; %s))", iterExpr, matcher, initExpr, updateExpr, extractExpr);
		}
	}
}
