package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AbstractBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Assignment<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;

	@Override
	public Cardinality getCardinality() {
		return rhs.getCardinality();
	}

	public Assignment(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, Version version, int lhsOutputIndex, int rhsOutputIndex) {
		super(lhs, rhs, lhsOutputIndex, rhsOutputIndex);
		this.jsonProvider = jsonProvider;
		this.version = version;
	}

	@Override
	protected AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs) {
		return new Assignment<>(jsonProvider, rewrittenLhs, rewrittenRhs, version, lhsOutputIndex, rhsOutputIndex);
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
		Memory memory = frame.getEnclosingMemory();
		rhs.apply(frame, in, UntrackedPath.getInstance(), (rval, opath) -> {
			memory.countOutput(rhsOutputIndex);
			List<Path<JsonNode>> lpaths = new ArrayList<>();
			lhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
				memory.countOutput(lhsOutputIndex);
				@Var Path<JsonNode> lpath = lpath0;
				// `VALUE | path(VALUE) => []`
				if (PathUtils.isLost(lpath) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (PathUtils.isLost(lpath))
					throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval)));
				lpaths.add(lpath);
			});
			@Var JsonNode out = in;
			RuntimeLimits limits = frame.getRuntimeLimits();
			for (Path<JsonNode> lpath : lpaths)
				out = PathOperations.mutate(jsonProvider, limits, lpath, out, (lval_) -> rval, version);
			output.emit(out, UntrackedPath.getInstance());
		});
	}
}
