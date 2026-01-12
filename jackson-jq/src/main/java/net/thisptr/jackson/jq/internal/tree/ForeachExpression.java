package net.thisptr.jackson.jq.internal.tree;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.path.Path;

public class ForeachExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> updateExpr;
	private Expression<JsonNode> initExpr;
	private Expression<JsonNode> extractExpr;
	private PatternMatcher<JsonNode> matcher;

	public ForeachExpression(final PatternMatcher<JsonNode> matcher, final Expression<JsonNode> initExpr, final Expression<JsonNode> updateExpr, final Expression<JsonNode> extractExpr, final Expression<JsonNode> iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {

		initExpr.apply(scope, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			final JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			final Path[] accumulatorPaths = new Path[] { accumulatorPath };

			final Scope<JsonNode> childScope = Scope.newChildScope(scope);

			iterExpr.apply(scope, in, ipath, (item, itemPath) -> {
				final Stack<MatchWithPath<JsonNode>> stack = new Stack<>();
				matcher.matchWithPath(scope, item, itemPath, (final List<MatchWithPath<JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						final MatchWithPath<JsonNode> var = vars.get(i);
						childScope.setValueWithPath(var.name, var.value, var.path);
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
