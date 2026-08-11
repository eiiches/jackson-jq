package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

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
