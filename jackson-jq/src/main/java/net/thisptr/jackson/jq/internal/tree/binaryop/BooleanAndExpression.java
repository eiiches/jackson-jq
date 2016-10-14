package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class BooleanAndExpression extends BinaryOperatorExpression {
	public BooleanAndExpression(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, "and");
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode l : lhs.apply(scope, in)) {
			if (!JsonNodeUtils.asBoolean(l)) {
				out.add(BooleanNode.FALSE);
				continue;
			}
			for (final JsonNode r : rhs.apply(scope, in))
				out.add(BooleanNode.valueOf(JsonNodeUtils.asBoolean(r)));
		}
		return out;
	}
}
