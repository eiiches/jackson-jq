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

public abstract class AbstractComplexAssignment<JsonNode> extends AbstractBinaryOperatorExpression<JsonNode> {
	protected final JsonProvider<JsonNode> jsonProvider;
	protected final Version version;
	private final boolean inputFixed;

	@Override
	public Cardinality getCardinality() {
		return rhs.getCardinality();
	}

	public AbstractComplexAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, boolean inputFixed) {
		super(lhs, rhs);
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.inputFixed = inputFixed;
	}

	protected abstract JsonNode eval(JsonNode lhs, JsonNode rhs) throws JsonQueryException;

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
				if (PathUtils.isLost(lpath) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (PathUtils.isLost(lpath))
					throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval)));
				lpaths.add(lpath);
			});
			@Var JsonNode out = in;
			for (Path<JsonNode> lpath : lpaths)
				out = PathOperations.mutate(jsonProvider, lpath, out, (lval) -> eval(lval == null ? jsonProvider.createNull() : lval, rval), version);
			output.emit(out, UntrackedPath.getInstance());
		});
	}
}
