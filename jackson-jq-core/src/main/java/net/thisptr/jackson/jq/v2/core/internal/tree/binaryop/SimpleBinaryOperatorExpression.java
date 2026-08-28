package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class SimpleBinaryOperatorExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private BinaryOperator<JsonNode> operator;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(lhs.getCardinality(), rhs.getCardinality());
	}

	public SimpleBinaryOperatorExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image());
		this.jsonProvider = jsonProvider;
		this.operator = operator;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		rhs.apply(frame, in, UntrackedPath.getInstance(), (r, opath) -> {
			lhs.apply(frame, in, UntrackedPath.getInstance(), (l, opath2) -> {
				output.emit(operator.apply(jsonProvider, l, r), UntrackedPath.getInstance());
			});
		});
	}
}
