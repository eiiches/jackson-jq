package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class ComplexAssignment<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private BinaryOperator<JsonNode> operator;

	public ComplexAssignment(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs, final BinaryOperator<JsonNode> operator) {
		super(lhs, rhs, operator.image() + "=");
		this.operator = operator;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		rhs.apply(scope, in, (rval) -> {
			final List<Path<JsonNode>> lpaths = new ArrayList<>();
			lhs.apply(scope, in, RootPath.getInstance(), (lval, lpath) -> {
				// `VALUE | path(VALUE) => []`
				if (lpath == null && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (lpath == null)
					throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval));
				lpaths.add(lpath);
			}, true);
			JsonNode out = in;
			for (final Path<JsonNode> lpath : lpaths)
				out = lpath.mutate(jsonProvider, out, (lval) -> operator.apply(jsonProvider, lval == null ? jsonProvider.createNull() : lval, rval));
			output.emit(out, null);
		});
	}
}
