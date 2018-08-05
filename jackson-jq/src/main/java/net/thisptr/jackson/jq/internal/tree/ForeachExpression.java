package net.thisptr.jackson.jq.internal.tree;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

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
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {

		initExpr.apply(scope, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			final JsonNode[] accumulators = new JsonNode[] { accumulator };

			final Scope childScope = Scope.newChildScope(scope);

			iterExpr.apply(scope, in, (item) -> {
				final Stack<Pair<String, JsonNode>> stack = new Stack<>();
				matcher.match(scope, item, (final List<Pair<String, JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						final Pair<String, JsonNode> var = vars.get(i);
						childScope.setValue(var._1, var._2);
					}

					updateExpr.apply(childScope, accumulators[0], (newaccumulator) -> {
						if (extractExpr != null) {
							extractExpr.apply(childScope, newaccumulator, output);
						} else {
							output.emit(newaccumulator);
						}
						accumulators[0] = newaccumulator;
					});
				}, stack, true);
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
