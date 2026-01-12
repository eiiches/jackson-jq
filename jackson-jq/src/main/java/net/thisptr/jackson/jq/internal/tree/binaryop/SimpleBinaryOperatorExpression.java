package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.path.Path;

public abstract class SimpleBinaryOperatorExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private BinaryOperator<JsonNode> operator;

	public SimpleBinaryOperatorExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs, final BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image());
		this.operator = operator;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		rhs.apply(scope, in, (r) -> {
			lhs.apply(scope, in, (l) -> {
				output.emit(operator.apply(scope.jsonProvider(), l, r), null);
			});
		});
	}
}
