package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ForeachExpression<JsonNode> implements Expression {
	private Expression iterExpr;
	private Expression updateExpr;
	private Expression initExpr;
	private @Nullable Expression extractExpr;
	private PatternMatcher<JsonNode> matcher;
	private java.util.Map<String, Integer> slots;

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression initExpr, Expression updateExpr, @Nullable Expression extractExpr, Expression iterExpr) {
		this(matcher, initExpr, updateExpr, extractExpr, iterExpr, java.util.Collections.emptyMap());
	}

	public ForeachExpression(PatternMatcher<JsonNode> matcher, Expression initExpr, Expression updateExpr, @Nullable Expression extractExpr, Expression iterExpr, java.util.Map<String, Integer> slots) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
		this.slots = slots;
	}

	public PatternMatcher<JsonNode> matcher() { return matcher; }
	public Expression initExpr() { return initExpr; }
	public Expression updateExpr() { return updateExpr; }
	public @Nullable Expression extractExpr() { return extractExpr; }
	public Expression iterExpr() { return iterExpr; }

	public int getSlot(String name) {
		Integer slot = slots.get(name);
		return slot != null ? slot.intValue() : -1;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((Scope) scope, (JsonNode) in, (Path) ipath, (PathOutput) output, requirePath);
	}

	private void applyInternal(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {

		initExpr.apply(scope, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			Path[] accumulatorPaths = new Path[] { accumulatorPath };

			Scope<JsonNode> childScope = Scope.newChildScope(scope);

			iterExpr.apply(scope, in, ipath, (item, itemPath) -> {
				Stack<MatchWithPath<JsonNode>> stack = new Stack<>();
				matcher.matchWithPath(scope, item, itemPath, (List<MatchWithPath<JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						MatchWithPath<JsonNode> var = vars.get(i);
						int slot = getSlot(var.name);
						childScope.setValueWithPath(slot, var.value, var.path, slots.size());
					}

					updateExpr.apply(childScope, accumulators[0], accumulatorPaths[0], (newaccumulator, newaccumulatorPath) -> {
						if (extractExpr != null) {
							extractExpr.apply(childScope, newaccumulator, newaccumulatorPath, output, requirePath);
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
