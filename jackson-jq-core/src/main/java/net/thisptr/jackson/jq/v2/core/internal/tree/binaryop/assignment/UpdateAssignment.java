package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryUndefinedBehaviorException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AbstractBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class UpdateAssignment<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	public UpdateAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, int lhsOutputIndex, int rhsOutputIndex) {
		super(lhs, rhs, lhsOutputIndex, rhsOutputIndex);
		this.jsonProvider = jsonProvider;
		this.version = version;
	}

	@Override
	protected Expression<StackFrame, JsonNode> recreate(Expression<StackFrame, JsonNode> rewrittenLhs, Expression<StackFrame, JsonNode> rewrittenRhs) {
		return new UpdateAssignment<>(jsonProvider, rewrittenLhs, rewrittenRhs, version, lhsOutputIndex, rhsOutputIndex);
	}

	// Always: an assignment applies its lhs path to the base `.` and falls back to the raw, unmodified
	// input when the lhs matches no paths, so it reads `.` even when neither child does. An enclosing
	// construct that rebinds `.` to a fixed value discharges this the same way it discharges any other
	// input dependency. External-state and free-variable dependencies need no override: they are fully
	// covered by super's `lhs || rhs`.
	@Override
	public boolean dependsOnInput() {
		return true;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		@SuppressWarnings("unchecked")
		JsonNode[] out = (JsonNode[]) new Object[] { in };
		Memory memory = frame.getEnclosingMemory();
		lhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
			memory.countOutput(lhsOutputIndex);
			@Var Path<JsonNode> lpath = lpath0;
			// `VALUE | path(VALUE) => []`
			if (PathUtils.isLost(lpath) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
				lpath = RootPath.getInstance();
			if (PathUtils.isLost(lpath))
				throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval)));

			out[0] = PathOperations.mutate(jsonProvider, frame.getRuntimeLimits(), lpath, out[0], (lval_) -> {
				List<JsonNode> rvals = new ArrayList<>();
				rhs.apply(frame, lval_, UntrackedPath.getInstance(), (v, opath) -> {
					memory.countOutput(rhsOutputIndex);
					rvals.add(v);
				});
				if (rvals.isEmpty())
					throw new JsonQueryUndefinedBehaviorException("`|= empty` is undefined. See https://github.com/stedolan/jq/issues/897");
				if (version.compareTo(Versions.JQ_1_6) >= 0) {
					return rvals.get(0);
				} else {
					return rvals.get(rvals.size() - 1);
				}
			}, version);
		});
		output.emit(out[0], UntrackedPath.getInstance());
	}
}
