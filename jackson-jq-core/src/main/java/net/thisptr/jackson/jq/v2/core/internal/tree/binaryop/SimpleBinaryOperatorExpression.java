package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class SimpleBinaryOperatorExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private BinaryOperator<JsonNode> operator;

	public SimpleBinaryOperatorExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs, BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image());
		this.jsonProvider = jsonProvider;
		this.operator = operator;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		rhs.apply(frame, in, (r) -> {
			lhs.apply(frame, in, (l) -> {
				output.emit(operator.apply(jsonProvider, l, r), null);
			});
		});
	}
}
