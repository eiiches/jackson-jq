package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class ForeachExpression extends JsonQuery {
	private JsonQuery iterExpr;
	private JsonQuery updateExpr;
	private JsonQuery initExpr;
	private String var;
	private JsonQuery extractExpr;

	public ForeachExpression(final String var, final JsonQuery initExpr, final JsonQuery updateExpr, final JsonQuery extractExpr, final JsonQuery iterExpr) {
		this.var = var;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (JsonNode accumulator : initExpr.apply(scope, in)) {
			final Scope childScope = new Scope(scope);
			for (final JsonNode item : iterExpr.apply(scope, in)) {
				childScope.setValue(var, item);
				try {
					for (final JsonNode newaccumulator : updateExpr.apply(childScope, accumulator)) {
						if (extractExpr != null) {
							for (final JsonNode extract : extractExpr.apply(childScope, newaccumulator))
								out.add(extract);
						} else {
							out.add(newaccumulator);
						}
						accumulator = newaccumulator;
					}
				} catch (JsonQueryBreakException e) {/* ignore */}
			}
		}
		return out;
	}

	@Override
	public String toString() {
		if (extractExpr == null) {
			return String.format("(foreach %s as $%s (%s; %s))", iterExpr, var, initExpr, updateExpr);
		} else {
			return String.format("(foreach %s as $%s (%s; %s; %s))", iterExpr, var, initExpr, updateExpr, extractExpr);
		}
	}
}
