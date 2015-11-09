package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class ReduceExpression extends JsonQuery {
	private JsonQuery iterExpr;
	private JsonQuery reduceExpr;
	private JsonQuery initExpr;
	private String boundVarName;

	public ReduceExpression(final String boundVarName, final JsonQuery initExpr, final JsonQuery reduceExpr, final JsonQuery iterExpr) {
		this.boundVarName = boundVarName;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (JsonNode accumulator : initExpr.apply(scope, in)) {
			final Scope childScope = new Scope(scope);
			try {
				for (final JsonNode item : iterExpr.apply(scope, in)) {
					childScope.setValue(boundVarName, item);
					for (final JsonNode newvalue : reduceExpr.apply(childScope, accumulator))
						accumulator = newvalue;
				}
			} catch (JsonQueryBreakException e) { /* break */}
			out.add(accumulator);
		}
		return out;
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as $%s (%s; %s))", iterExpr, boundVarName, initExpr, reduceExpr);
	}
}
