package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ReduceExpression implements Expression {
	private Expression iterExpr;
	private Expression reduceExpr;
	private Expression initExpr;
	private PatternMatcher matcher;

	public ReduceExpression(final PatternMatcher matcher, final Expression initExpr, final Expression reduceExpr, final Expression iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		initExpr.apply(scope, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			final JsonNode[] accumulators = new JsonNode[] { accumulator };

			try {

				final Scope childScope = Scope.newChildScope(scope);
				iterExpr.apply(scope, in, (item) -> {
					final Stack<Pair<String, JsonNode>> stack = new Stack<>();
					matcher.match(scope, item, (final List<Pair<String, JsonNode>> vars) -> {
						for (int i = vars.size() - 1; i >= 0; --i) {
							final Pair<String, JsonNode> var = vars.get(i);
							childScope.setValue(var._1, var._2);
						}

						// We only use the last value from reduce expression.
						final List<JsonNode> reduceResult = new ArrayList<>();
						reduceExpr.apply(childScope, accumulators[0], reduceResult::add);
						accumulators[0] = reduceResult.isEmpty() ? NullNode.getInstance() : reduceResult.get(reduceResult.size() - 1);
					}, stack, true);
				});

			} finally {
				output.emit(accumulators[0]);
			}
		});
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}