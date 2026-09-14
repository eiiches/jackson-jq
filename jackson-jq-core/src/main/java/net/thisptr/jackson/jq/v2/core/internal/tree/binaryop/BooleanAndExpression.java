package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class BooleanAndExpression<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(lhs.getCardinality(), rhs.getCardinality());
	}

	public BooleanAndExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs) {
		super(lhs, rhs);
		this.jsonProvider = jsonProvider;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		lhs.apply(frame, in, UntrackedPath.getInstance(), (l, opath) -> {
			if (!JsonNodeUtils.asBoolean(jsonProvider, l)) {
				output.emit(jsonProvider.createBoolean(false), UntrackedPath.getInstance());
				return;
			}
			rhs.apply(frame, in, UntrackedPath.getInstance(), (r, opath2) -> {
				output.emit(jsonProvider.createBoolean(JsonNodeUtils.asBoolean(jsonProvider, r)), UntrackedPath.getInstance());
			});
		});
	}
}
