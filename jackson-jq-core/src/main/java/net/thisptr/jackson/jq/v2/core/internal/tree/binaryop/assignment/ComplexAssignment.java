package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ComplexAssignment<JsonNode> extends BinaryOperatorExpression {
	private BinaryOperator<JsonNode> operator;

	public ComplexAssignment(Expression lhs, Expression rhs, BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image() + "=");
		this.operator = operator;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame frame, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((JsonProvider) jsonProvider, (ExecutionStack.Frame) frame, (JsonNode) in, (Path) ipath, (PathOutput) output, requirePath);
	}

	private void applyInternal(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		rhs.apply(jsonProvider, frame, in, (rval) -> {
			List<Path<JsonNode>> lpaths = new ArrayList<>();
			lhs.apply(jsonProvider, frame, in, RootPath.getInstance(), (lval, lpath0) -> {
				@Var Path<JsonNode> lpath = lpath0;
				// `VALUE | path(VALUE) => []`
				if (lpath == null && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (lpath == null)
					throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval));
				lpaths.add(lpath);
			}, true);
			@Var JsonNode out = in;
			for (Path<JsonNode> lpath : lpaths)
				out = lpath.mutate(jsonProvider, out, (lval) -> operator.apply(jsonProvider, lval == null ? jsonProvider.createNull() : lval, rval));
			output.emit(out, null);
		});
	}
}
