package net.thisptr.jackson.jq.internal.tree.binaryop;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;

public abstract class SimpleBinaryOperatorExpression extends BinaryOperatorExpression {
	private BinaryOperator operator;

	public SimpleBinaryOperatorExpression(final Expression lhs, final Expression rhs, final BinaryOperator operator) {
		super(lhs, rhs, operator.image());
		this.operator = operator;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		rhs.apply(scope, in, (r) -> {
			lhs.apply(scope, in, (l) -> {
				output.emit(operator.apply(scope.getObjectMapper(), l, r));
			});
		});
	}
}
