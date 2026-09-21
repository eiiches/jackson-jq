package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class BooleanOrExpression<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(lhs.getCardinality(), rhs.getCardinality());
	}

	public BooleanOrExpression(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		super(lhs, rhs, lhsOutputIndex, rhsOutputIndex);
		this.jsonProvider = jsonProvider;
	}

	@Override
	protected AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs) {
		return new BooleanOrExpression<>(jsonProvider, rewrittenLhs, rewrittenRhs, lhsOutputIndex, rhsOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		lhs.apply(frame, in, UntrackedPath.getInstance(), (l, opath) -> {
			memory.countOutput(lhsOutputIndex);
			if (JsonNodeUtils.asBoolean(jsonProvider, l)) {
				output.emit(jsonProvider.createBoolean(true), UntrackedPath.getInstance());
				return;
			}
			rhs.apply(frame, in, UntrackedPath.getInstance(), (r, opath2) -> {
				memory.countOutput(rhsOutputIndex);
				output.emit(jsonProvider.createBoolean(JsonNodeUtils.asBoolean(jsonProvider, r)), UntrackedPath.getInstance());
			});
		});
	}
}
