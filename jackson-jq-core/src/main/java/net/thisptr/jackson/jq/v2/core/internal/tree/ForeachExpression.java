package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ForeachExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> updateExpr;
	private Expression<JsonNode> initExpr;
	private @Nullable Expression<JsonNode> extractExpr;
	private PatternMatcher<JsonNode> matcher;
	private java.util.Map<String, Integer> slots;

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression<JsonNode> initExpr, Expression<JsonNode> updateExpr, @Nullable Expression<JsonNode> extractExpr, Expression<JsonNode> iterExpr) {
		this(matcher, initExpr, updateExpr, extractExpr, iterExpr, java.util.Collections.emptyMap());
	}

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression<JsonNode> initExpr, Expression<JsonNode> updateExpr, @Nullable Expression<JsonNode> extractExpr, Expression<JsonNode> iterExpr, java.util.Map<String, Integer> slots) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
		this.slots = slots;
	}

	public PatternMatcher<JsonNode> matcher() { return matcher; }
	public Expression<JsonNode> initExpr() { return initExpr; }
	public Expression<JsonNode> updateExpr() { return updateExpr; }
	public @Nullable Expression<JsonNode> extractExpr() { return extractExpr; }
	public Expression<JsonNode> iterExpr() { return iterExpr; }

	public int getSlot(String name) {
		Integer slot = slots.get(name);
		return slot != null ? slot.intValue() : -1;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		initExpr.apply(frame, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			@SuppressWarnings("unchecked")
			Path<JsonNode>[] accumulatorPaths = (Path<JsonNode>[]) new Path<?>[] { accumulatorPath };

			iterExpr.apply(frame, in, ipath, (item, itemPath) -> {
				Stack<MatchWithPath<JsonNode>> stack = new Stack<>();
				matcher.matchWithPath(frame, item, itemPath, (List<MatchWithPath<JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						MatchWithPath<JsonNode> var = vars.get(i);
						int slot = getSlot(var.name);
						if (frame != null && slot >= 0) {
							frame.set(slot, var.path, var.value);
						}
					}

					updateExpr.apply(frame, accumulators[0], accumulatorPaths[0], (newaccumulator, newaccumulatorPath) -> {
						if (extractExpr != null) {
							extractExpr.apply(frame, newaccumulator, newaccumulatorPath, output, requirePath);
						} else {
							output.emit(newaccumulator, newaccumulatorPath);
						}
						accumulators[0] = newaccumulator;
						accumulatorPaths[0] = newaccumulatorPath;
					}, extractExpr != null ? false : requirePath);
				}, stack);
			}, requirePath);
		}, false);
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
