package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.Path;

public class ReduceExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> reduceExpr;
	private Expression<JsonNode> initExpr;
	private PatternMatcher<JsonNode> matcher;

	public ReduceExpression(final PatternMatcher<JsonNode> matcher, final Expression<JsonNode> initExpr, final Expression<JsonNode> reduceExpr, final Expression<JsonNode> iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		initExpr.apply(scope, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			final JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };

			final Scope<JsonNode> childScope = Scope.newChildScope(scope);
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
					accumulators[0] = reduceResult.isEmpty() ? scope.jsonProvider().createNull() : reduceResult.get(reduceResult.size() - 1);
				}, stack);
			});

			output.emit(accumulators[0], null);
		});
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}