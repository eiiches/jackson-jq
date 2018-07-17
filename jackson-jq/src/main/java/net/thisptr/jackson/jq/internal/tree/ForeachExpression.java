package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ForeachExpression extends JsonQuery {
	private JsonQuery iterExpr;
	private JsonQuery updateExpr;
	private JsonQuery initExpr;
	private JsonQuery extractExpr;
	private PatternMatcher matcher;

	public ForeachExpression(final PatternMatcher matcher, final JsonQuery initExpr, final JsonQuery updateExpr, final JsonQuery extractExpr, final JsonQuery iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();

		try {

			for (final JsonNode accumulator : initExpr.apply(scope, in)) {
				// Wrap in array to allow mutation inside lambda
				final JsonNode[] accumulators = new JsonNode[] { accumulator };

				final Scope childScope = Scope.newChildScope(scope);
				for (final JsonNode item : iterExpr.apply(scope, in)) {

					final Stack<Pair<String, JsonNode>> stack = new Stack<>();
					matcher.match(scope, item, (final List<Pair<String, JsonNode>> vars) -> {
						for (int i = vars.size() - 1; i >= 0; --i) {
							final Pair<String, JsonNode> var = vars.get(i);
							childScope.setValue(var._1, var._2);
						}

						for (final JsonNode newaccumulator : updateExpr.apply(childScope, accumulators[0])) {
							if (extractExpr != null) {
								for (final JsonNode extract : extractExpr.apply(childScope, newaccumulator))
									out.add(extract);
							} else {
								out.add(newaccumulator);
							}
							accumulators[0] = newaccumulator;
						}
					}, stack, true);
				}
			}

		} catch (JsonQueryBreakException e) {
			/* ignore */
		}

		return out;
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
