package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AbstractBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Assignment<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final boolean inputFixed;

	@Override
	public Cardinality getCardinality() {
		return rhs.getCardinality();
	}

	public Assignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, boolean inputFixed) {
		super(lhs, rhs);
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.inputFixed = inputFixed;
	}

	// Falls back to the raw, unmodified `in` when lhs matches no paths, so the result always
	// incorporates the base `.` being mutated -- unlike super's plain `lhs || rhs`, this also
	// requires `.` itself to be known input-independent. External-state and free-variable dependencies
	// need no override: they are fully covered by super's `lhs || rhs`, since the surrounding pipe (if any)
	// already accounts for whatever the base `.` might carry.
	@Override
	public boolean dependsOnInput() {
		return !inputFixed || super.dependsOnInput();
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		rhs.apply(frame, in, UntrackedPath.getInstance(), (rval, opath) -> {
			List<Path<JsonNode>> lpaths = new ArrayList<>();
			lhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
				@Var Path<JsonNode> lpath = lpath0;
				// `VALUE | path(VALUE) => []`
				if (PathUtils.isLost(lpath) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (PathUtils.isLost(lpath))
					throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval)));
				lpaths.add(lpath);
			});
			@Var JsonNode out = in;
			for (Path<JsonNode> lpath : lpaths)
				out = PathOperations.mutate(jsonProvider, lpath, out, (lval_) -> rval, version);
			output.emit(out, UntrackedPath.getInstance());
		});
	}
}
