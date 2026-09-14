package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.concurrent.atomic.AtomicBoolean;

import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class AlternativeOperatorExpression<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.alternative(lhs.getCardinality(), rhs.getCardinality());
	}

	public AlternativeOperatorExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> valueExpr, Expression<StackFrame, JsonNode> defaultExpr) {
		super(valueExpr, defaultExpr);
		this.jsonProvider = jsonProvider;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		AtomicBoolean emitted = new AtomicBoolean();
		lhs.apply(frame, in, path, (out, outpath) -> {
			if (JsonNodeUtils.asBoolean(jsonProvider, out)) {
				output.emit(out, outpath);
				emitted.set(true);
			}
		});
		if (!emitted.get()) {
			rhs.apply(frame, in, path, output);
		}
	}
}
