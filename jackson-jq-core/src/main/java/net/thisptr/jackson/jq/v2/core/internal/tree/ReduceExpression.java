package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ReduceExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> reduceExpr;
	private Expression<JsonNode> initExpr;
	private PatternMatcher<JsonNode> matcher;

	public ReduceExpression(PatternMatcher<JsonNode> matcher, Expression<JsonNode> initExpr, Expression<JsonNode> reduceExpr, Expression<JsonNode> iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		initExpr.apply(scope, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };

			Scope<JsonNode> childScope = Scope.newChildScope(scope);
			iterExpr.apply(scope, in, (item) -> {
				Stack<Pair<String, JsonNode>> stack = new Stack<>();
				matcher.match(scope, item, (List<Pair<String, JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						Pair<String, JsonNode> var = vars.get(i);
						childScope.setValue(var._1, var._2);
					}

					// We only use the last value from reduce expression.
					List<JsonNode> reduceResult = new ArrayList<>();
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
