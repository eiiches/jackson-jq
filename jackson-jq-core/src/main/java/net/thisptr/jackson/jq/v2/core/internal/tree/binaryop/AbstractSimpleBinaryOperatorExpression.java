package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractSimpleBinaryOperatorExpression<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	protected final JsonProvider<JsonNode> jsonProvider;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(lhs.getCardinality(), rhs.getCardinality());
	}

	public AbstractSimpleBinaryOperatorExpression(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		super(lhs, rhs, lhsOutputIndex, rhsOutputIndex);
		this.jsonProvider = jsonProvider;
	}

	protected abstract JsonNode doEval(RuntimeLimits limits, JsonNode lhs, JsonNode rhs) throws JsonQueryException;

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		rhs.apply(frame, in, UntrackedPath.getInstance(), (r, opath) -> {
			memory.countOutput(rhsOutputIndex);
			lhs.apply(frame, in, UntrackedPath.getInstance(), (l, opath2) -> {
				memory.countOutput(lhsOutputIndex);
				output.emit(doEval(frame.getRuntimeLimits(), l, r), UntrackedPath.getInstance());
			});
		});
	}
}
