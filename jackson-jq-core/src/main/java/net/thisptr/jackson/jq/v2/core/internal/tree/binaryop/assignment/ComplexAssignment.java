package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.internal.path.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ComplexAssignment<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private BinaryOperator<JsonNode> operator;
	private final Version version;
	private final boolean inputFixed;

	@Override
	public Cardinality getCardinality() {
		return rhs.getCardinality();
	}

	public ComplexAssignment(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, BinaryOperator<JsonNode> operator, Version version, boolean inputFixed) {
		super(lhs, rhs, operator.image() + "=");
		this.jsonProvider = jsonProvider;
		this.operator = operator;
		this.version = version;
		this.inputFixed = inputFixed;
	}

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
				out = PathOperations.mutate(jsonProvider, lpath, out, (lval) -> operator.apply(jsonProvider, lval == null ? jsonProvider.createNull() : lval, rval), version);
			output.emit(out, UntrackedPath.getInstance());
		});
	}
}
