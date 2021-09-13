package net.thisptr.jackson.jq.internal.tree;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.path.Path;

public class ForeachExpression implements Expression {
	private Expression iterExpr;
	private Expression updateExpr;
	private Expression initExpr;
	private Expression extractExpr;
	private PatternMatcher matcher;

	public ForeachExpression(final PatternMatcher matcher, final Expression initExpr, final Expression updateExpr, final Expression extractExpr, final Expression iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {

		initExpr.apply(scope, in, ipath, (accumulator, accumulatorPath) -> {
			// Wrap in array to allow mutation inside lambda
			final JsonNode[] accumulators = new JsonNode[] { accumulator };
			final Path[] accumulatorPaths = new Path[] { accumulatorPath };

			final Scope childScope = Scope.newChildScope(scope);

			iterExpr.apply(scope, in, ipath, (item, itemPath) -> {
				final Stack<MatchWithPath> stack = new Stack<>();
				matcher.matchWithPath(scope, item, itemPath, (final List<MatchWithPath> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						final MatchWithPath var = vars.get(i);
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
