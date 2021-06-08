package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ReduceExpression extends JsonQuery {
	private JsonQuery iterExpr;
	private JsonQuery reduceExpr;
	private JsonQuery initExpr;
	private PatternMatcher matcher;

	public ReduceExpression(final PatternMatcher matcher, final JsonQuery initExpr, final JsonQuery reduceExpr, final JsonQuery iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();

		try {

			for (final JsonNode accumulator : initExpr.apply(scope, in)) {
				// Wrap in array to allow mutation inside lambda
				final JsonNode[] accumulators = new JsonNode[] { accumulator };

				try {

					final Scope childScope = Scope.newChildScope(scope);
					for (final JsonNode item : iterExpr.apply(scope, in)) {
						final Stack<Pair<String, JsonNode>> stack = new Stack<>();
						matcher.match(scope, item, (final List<Pair<String, JsonNode>> vars) -> {
							for (int i = vars.size() - 1; i >= 0; --i) {
								final Pair<String, JsonNode> var = vars.get(i);
								childScope.setValue(var._1, var._2);
							}

							// We only use the last value from reduce expression.
							final List<JsonNode> reduceResult = reduceExpr.apply(childScope, accumulators[0]);
							accumulators[0] = reduceResult.isEmpty() ? NullNode.getInstance() : reduceResult.get(reduceResult.size() - 1);
						}, stack);
					}

				} finally {
					out.add(accumulators[0]);
				}
			}

		} catch (JsonQueryBreakException e) {
			/* ignore */
		}

		return out;
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}