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
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ComplexAssignment<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private BinaryOperator<JsonNode> operator;

	public ComplexAssignment(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs, BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image() + "=");
		this.jsonProvider = jsonProvider;
		this.operator = operator;
	}

	@Override
	public void apply(@Nullable StackFrame<JsonNode> frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		rhs.apply(frame, in, (rval) -> {
			List<Path<JsonNode>> lpaths = new ArrayList<>();
			lhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
				@Var Path<JsonNode> lpath = lpath0;
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
