package net.thisptr.jackson.jq.internal.tree.binaryop;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.path.Path;

public abstract class SimpleBinaryOperatorExpression extends BinaryOperatorExpression {
	private BinaryOperator operator;

	public SimpleBinaryOperatorExpression(final Expression lhs, final Expression rhs, final BinaryOperator operator) {
		super(lhs, rhs, operator.image());
		this.operator = operator;
	}

	public SimpleBinaryOperatorExpression() {}

	public BinaryOperator getOperator() {
		return operator;
	}

	public void setOperator(BinaryOperator operator) {
		this.operator = operator;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		rhs.apply(scope, in, (r) -> {
			lhs.apply(scope, in, (l) -> {
				output.emit(operator.apply(scope.getObjectMapper(), l, r), null);
			});
		});
	}
}
