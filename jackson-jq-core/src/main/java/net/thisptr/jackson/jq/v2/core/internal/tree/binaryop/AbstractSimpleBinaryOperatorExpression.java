package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractSimpleBinaryOperatorExpression<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	protected final JsonProvider<JsonNode> jsonProvider;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(lhs.getCardinality(), rhs.getCardinality());
	}

	public AbstractSimpleBinaryOperatorExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs) {
		super(lhs, rhs);
		this.jsonProvider = jsonProvider;
	}

	protected abstract JsonNode doEval(JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		rhs.apply(frame, in, UntrackedPath.getInstance(), (r, opath) -> {
			lhs.apply(frame, in, UntrackedPath.getInstance(), (l, opath2) -> {
				output.emit(doEval(l, r), UntrackedPath.getInstance());
			});
		});
	}
}
