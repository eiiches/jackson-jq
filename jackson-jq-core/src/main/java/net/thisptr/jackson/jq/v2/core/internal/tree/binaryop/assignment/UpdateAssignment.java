package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryUndefinedBehaviorException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
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
	private Version version;
	private final boolean inputFixed;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	public UpdateAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, boolean inputFixed) {
		super(lhs, rhs);
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.inputFixed = inputFixed;
	}

	@Override
	public boolean dependsOnInput() {
		return !inputFixed || super.dependsOnInput();
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		@SuppressWarnings("unchecked")
		JsonNode[] out = (JsonNode[]) new Object[] { in };
		lhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
			@Var Path<JsonNode> lpath = lpath0;
			// `VALUE | path(VALUE) => []`
			if (PathUtils.isLost(lpath) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
				lpath = RootPath.getInstance();
			if (PathUtils.isLost(lpath))
				throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval)));

			out[0] = PathOperations.mutate(jsonProvider, lpath, out[0], (lval_) -> {
				List<JsonNode> rvals = new ArrayList<>();
				rhs.apply(frame, lval_, UntrackedPath.getInstance(), (v, opath) -> rvals.add(v));
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
