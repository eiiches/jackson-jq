package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class SimpleBinaryOperatorExpression extends BinaryOperatorExpression {
	private BinaryOperator operator;

	public SimpleBinaryOperatorExpression(final JsonQuery lhs, final JsonQuery rhs, final BinaryOperator operator) {
		super(lhs, rhs, operator.image());
		this.operator = operator;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode l : lhs.apply(scope, in))
			for (final JsonNode r : rhs.apply(scope, in))
				out.add(operator.apply(scope.getObjectMapper(), l, r));
		return out;
	}
}
