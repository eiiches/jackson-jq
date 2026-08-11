package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

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
