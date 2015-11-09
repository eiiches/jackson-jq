package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;

public class AlternativeOperator extends BinaryOperatorExpression {
	public AlternativeOperator(final JsonQuery valueExpr, final JsonQuery defaultExpr) {
		super(valueExpr, defaultExpr, "//");
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode i : lhs.apply(scope, in)) {
			if (JsonNodeUtils.asBoolean(i))
				out.add(i);
		}
		if (out.isEmpty())
			out.addAll(rhs.apply(scope, in));
		return out;
	}
}
